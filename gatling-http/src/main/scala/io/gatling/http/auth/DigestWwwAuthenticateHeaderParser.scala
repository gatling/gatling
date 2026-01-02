/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.util.Locale

import scala.util.parsing.combinator.RegexParsers

import io.gatling.commons.validation._
import io.gatling.http.auth.DigestAuth.Algorithm
import io.gatling.http.client.uri.Uri

private[auth] object DigestWwwAuthenticateHeaderParser {
  private val AttributeNameRegex = "[a-z]+".r
  private val QuotedAttributeValueRegex = "[^\"]+".r
  private val UnquotedAttributeValueRegex = "[^\\s,]+".r
}

private[auth] final class DigestWwwAuthenticateHeaderParser extends RegexParsers {
  import DigestWwwAuthenticateHeaderParser._

  private val attributeName: Parser[String] = AttributeNameRegex
  private val quotedAttributeValue: Parser[String] = "\"" ~> QuotedAttributeValueRegex <~ "\""
  private val unquotedAttributeValue: Parser[String] = UnquotedAttributeValueRegex
  private val attributeValue: Parser[String] = quotedAttributeValue | unquotedAttributeValue
  private val attribute: Parser[(String, String)] = ((attributeName <~ "=") ~ attributeValue) ^^ { case name ~ value => name -> value }
  private val parser: Parser[Map[String, String]] = ("Digest " ~> repsep(attribute, ",")) ^^ {
    _.toMap
  }

  private def parseAlgorithm(algorithm: String): Option[DigestAuth.Algorithm] =
    algorithm match {
      case DigestAuth.Algorithm.Md5.value           => Some(DigestAuth.Algorithm.Md5)
      case DigestAuth.Algorithm.Md5Sess.value       => Some(DigestAuth.Algorithm.Md5Sess)
      case DigestAuth.Algorithm.Sha256.value        => Some(DigestAuth.Algorithm.Sha256)
      case DigestAuth.Algorithm.Sha256Sess.value    => Some(DigestAuth.Algorithm.Sha256Sess)
      case DigestAuth.Algorithm.Sha512256.value     => Some(DigestAuth.Algorithm.Sha512256)
      case DigestAuth.Algorithm.Sha512256Sess.value => Some(DigestAuth.Algorithm.Sha512256Sess)
      case _                                        => None
    }

  private def parseQop(qopOpt: Option[String]): Validation[DigestAuth.Qop] =
    qopOpt match {
      case Some(qop) =>
        val values = qop.split(",").map(_.trim)
        if (values.contains(DigestAuth.Qop.Auth.value)) {
          DigestAuth.Qop.Auth.success
        } else if (values.contains(DigestAuth.Qop.AuthInt.value)) {
          "Gatling doesn't support qop=auth-int".failure
        } else {
          DigestAuth.Qop.Auth.success
        }
      case _ => DigestAuth.Qop.Auth.success
    }

  private def parseDomain(domainOpt: Option[String], origin: Uri): Set[DigestAuth.ProtectionSpace] =
    domainOpt match {
      case Some(domain) =>
        domain
          .split(",")
          .map { url =>
            val uri = Uri.create(origin, url.trim)
            DigestAuth.ProtectionSpace(uri.getHost.toLowerCase(Locale.ROOT), uri.getLastDirectoryPath)
          }
          .toSet

      case _ => Set(DigestAuth.ProtectionSpace(origin.getHost.toLowerCase(Locale.ROOT), "/"))
    }

  def parse(header: String, origin: Uri): Validation[DigestAuth.Challenge] =
    safely() {
      parseAll(parser, header) match {
        case Success(attributes, _) =>
          for {
            realm <- attributes.get("realm").toValidation("realm attribute is missing")
            nonce <- attributes.get("nonce").toValidation("nonce attribute is missing")
            qop <- parseQop(attributes.get("qop"))
          } yield DigestAuth.Challenge(
            realm,
            parseDomain(attributes.get("domain"), origin),
            nonce,
            attributes.get("opaque"),
            attributes.get("stale").exists(_.toBoolean),
            attributes.get("algorithm").flatMap(parseAlgorithm).getOrElse(Algorithm.Md5),
            qop,
            attributes.get("userhash").exists(_.toBoolean)
          )
        case ns: NoSuccess => s"Failed to parse header $header: ${ns.msg.failure}".failure
      }
    }
}
