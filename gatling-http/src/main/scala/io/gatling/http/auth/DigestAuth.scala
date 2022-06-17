/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.auth

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ThreadLocalRandom

import io.gatling.commons.util.StringHelper.RichString
import io.gatling.commons.validation.Validation
import io.gatling.http.client.uri.Uri
import io.gatling.http.client.util.StringUtils.toHexString
import io.gatling.netty.util.StringBuilderPool

import io.netty.handler.codec.http.HttpMethod

object DigestAuth {

  private val ParserPool = new ThreadLocal[DigestWwwAuthenticateHeaderParser] {
    override def initialValue: DigestWwwAuthenticateHeaderParser = new DigestWwwAuthenticateHeaderParser
  }

  private val Md5DigestPool = new ThreadLocal[MessageDigest] {
    override def initialValue: MessageDigest = MessageDigest.getInstance("MD5")
  }

  private val Sha256DigestPool = new ThreadLocal[MessageDigest] {
    override def initialValue: MessageDigest = MessageDigest.getInstance("SHA-256")
  }

  private val Sha512256DigestPool = new ThreadLocal[MessageDigest] {
    override def initialValue: MessageDigest = MessageDigest.getInstance("SHA-512/256")
  }

  private val SbPool = new StringBuilderPool

  def parseWwwAuthenticateHeader(header: String, origin: Uri): Validation[Challenge] =
    ParserPool.get().parse(header, origin)

  private def newCnonce(): String = {
    val b = new Array[Byte](8)
    ThreadLocalRandom.current.nextBytes(b)
    Base64.getEncoder.encodeToString(b)
  }

  def hash(s: String, md: MessageDigest): String = toHexString(md.digest(s.getBytes(UTF_8)))

  def generateAuthorization(challenge: Challenge, username: String, password: String, requestMethod: HttpMethod, requestUri: Uri, nc: Int): String = {
    generateAuthorization0(challenge, username, password, requestMethod, requestUri, nc, newCnonce())
  }

  // for test only
  private[auth] def generateAuthorization0(
      challenge: Challenge,
      username: String,
      password: String,
      requestMethod: HttpMethod,
      requestUri: Uri,
      nc: Int,
      cnonce: String
  ): String = {
    val algorithm = challenge.algorithm
    val md = algorithm match {
      case Algorithm.Md5 | Algorithm.Md5Sess             => Md5DigestPool.get()
      case Algorithm.Sha256 | Algorithm.Sha256Sess       => Sha256DigestPool.get()
      case Algorithm.Sha512256 | Algorithm.Sha512256Sess => Sha512256DigestPool.get()
    }
    val qop = challenge.qop

    val hashedUsername = if (challenge.userhash) hash(s"$username:${challenge.realm}", md) else username
    val paddedNc = nc.toString.leftPad(8, "0")

    val a1 =
      if (challenge.algorithm.session) {
        // if -sess algo:     A1 = H( unq(username) ":" unq(realm) ":" passwd ) ":" unq(nonce-prime) ":" unq(cnonce-prime)
        s"${hash(s"$username:${challenge.realm}:$password", md)}:${challenge.nonce}:$cnonce"
      } else {
        // if standard algo:  A1 = unq(username) ":" unq(realm) ":" passwd
        s"$username:${challenge.realm}:$password"
      }

    // we don't support "auth-int" for now
    // if qop=auth        A2       = Method ":" request-uri
    // if qop=auth-int    A2       = Method ":" request-uri ":" H(entity-body)
    val a2 = s"${requestMethod.name}:${requestUri.toRelativeUrl}"

    // H(concat(H(A1), ":", unq(nonce), ":", nc, ":", unq(cnonce), ":", unq(qop), ":", H(A2)))
    val response = hash(s"${hash(a1, md)}:${challenge.nonce}:$paddedNc:$cnonce:${qop.value}:${hash(a2, md)}", md)

    val sb = SbPool
      .get()
      .append("Digest ")
      .append("username=\"")
      .append(hashedUsername)
      .append("\", realm=\"")
      .append(challenge.realm)
      .append("\", uri=\"")
      .append(requestUri.toRelativeUrl)
      .append("\", algorithm=")
      .append(algorithm.value)
      .append(", nonce=\"")
      .append(challenge.nonce)
      .append("\", nc=")
      .append(paddedNc)
      .append(", cnonce=\"")
      .append(cnonce)
      .append("\", qop=")
      .append(qop.value)
      .append(", response=\"")
      .append(response)
      .append("\"")

    challenge.opaque.foreach(opaque => sb.append(", opaque=\"").append(opaque).append("\""))

    if (challenge.userhash) {
      sb.append(", userhash=true")
    }

    sb.toString
  }

  sealed abstract class Qop(val value: String)

  object Qop {
    case object Auth extends Qop("auth")
    case object AuthInt extends Qop("auth-int")
  }

  sealed abstract class Algorithm(val value: String, val session: Boolean, val securityLevel: Int) extends Product with Serializable

  object Algorithm {
    case object Md5 extends Algorithm("MD5", session = false, 0)
    case object Md5Sess extends Algorithm("MD5-sess", session = true, 0)
    case object Sha256 extends Algorithm("SHA-256", session = false, 1)
    case object Sha256Sess extends Algorithm("SHA-256-sess", session = true, 1)
    case object Sha512256 extends Algorithm("SHA-512-256", session = false, 2)
    case object Sha512256Sess extends Algorithm("SHA-512-256-sess", session = true, 2)
  }

  final case class ProtectionSpace(domain: String, path: String) {
    def matches(uri: Uri): Boolean = uri.getHost == domain && uri.getNonEmptyPath.startsWith(path)
  }

  final case class Challenge(
      realm: String,
      domain: Set[ProtectionSpace],
      nonce: String,
      opaque: Option[String],
      stale: Boolean,
      algorithm: Algorithm,
      qop: Qop,
      userhash: Boolean
  )
}

final case class DigestAuth() {}
