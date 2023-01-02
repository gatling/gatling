/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.http.client.uri.Uri

import io.netty.handler.codec.http.HttpMethod

object DigestAuthSpec {
  implicit final class OneLine(val s: String) extends AnyVal {
    def oneLine: String = s.stripMargin.replace("\n", " ").replace("\r", "")
  }
}

class DigestAuthSpec extends BaseSpec with ValidationValues {
  import DigestAuthSpec._

  "parse" should "parse a simple well-formed header" in {
    DigestAuth
      .parseWwwAuthenticateHeader(
        """Digest
        |realm="realm",
        |nonce="nonce"""".oneLine,
        Uri.create("https://gatling.io")
      )
      .succeeded shouldBe
      DigestAuth.Challenge(
        realm = "realm",
        domain = Set(DigestAuth.ProtectionSpace("gatling.io", "/")),
        nonce = "nonce",
        opaque = None,
        stale = false,
        algorithm = DigestAuth.Algorithm.Md5,
        qop = DigestAuth.Qop.Auth,
        userhash = false
      )
  }

  it should "parse optional attributes" in {
    DigestAuth
      .parseWwwAuthenticateHeader(
        """Digest
        |realm="realm",
        |nonce="nonce",
        |opaque="opaque",
        |stale=false,
        |algorithm=SHA-256,
        |qop="auth"""".oneLine,
        Uri.create("https://gatling.io")
      )
      .succeeded shouldBe
      DigestAuth.Challenge(
        realm = "realm",
        domain = Set(DigestAuth.ProtectionSpace("gatling.io", "/")),
        nonce = "nonce",
        opaque = Some("opaque"),
        stale = false,
        algorithm = DigestAuth.Algorithm.Sha256,
        qop = DigestAuth.Qop.Auth,
        userhash = false
      )
  }

  "generateAuthorization" should "work with MD5" in {
    val wwwAuthenticateHeader = """Digest
                                  |realm="http-auth@example.org",
                                  |qop="auth, auth-int",
                                  |algorithm=MD5,
                                  |nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v",
                                  |opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS"""".oneLine

    val challenge = DigestAuth.parseWwwAuthenticateHeader(wwwAuthenticateHeader, Uri.create("http://example.org")).succeeded
    val cnonce = "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ"
    DigestAuth.generateAuthorization0(
      challenge = challenge,
      username = "Mufasa",
      password = "Circle of Life",
      requestMethod = HttpMethod.GET,
      requestUri = Uri.create("http://www.example.org/dir/index.html"),
      nc = 1,
      cnonce = cnonce
    ) shouldBe s"""Digest
                     |username="Mufasa",
                     |realm="http-auth@example.org",
                     |uri="/dir/index.html",
                     |algorithm=MD5,
                     |nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v",
                     |nc=00000001,
                     |cnonce="$cnonce",
                     |qop=auth,
                     |response="8ca523f5e9506fed4657c9700eebdbec",
                     |opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS"""".oneLine
  }

  it should "work with SHA-256" in {
    val wwwAuthenticateHeader = """Digest
                                  |realm="http-auth@example.org",
                                  |qop="auth, auth-int",
                                  |algorithm=SHA-256,
                                  |nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v",
                                  |opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS"""".oneLine

    val challenge = DigestAuth.parseWwwAuthenticateHeader(wwwAuthenticateHeader, Uri.create("http://example.org")).succeeded
    val cnonce = "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ"
    DigestAuth.generateAuthorization0(
      challenge = challenge,
      username = "Mufasa",
      password = "Circle of Life",
      requestMethod = HttpMethod.GET,
      requestUri = Uri.create("http://www.example.org/dir/index.html"),
      nc = 1,
      cnonce = cnonce
    ) shouldBe s"""Digest
                  |username="Mufasa",
                  |realm="http-auth@example.org",
                  |uri="/dir/index.html",
                  |algorithm=SHA-256,
                  |nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v",
                  |nc=00000001,
                  |cnonce="$cnonce",
                  |qop=auth,
                  |response="753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1",
                  |opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS"""".stripMargin.oneLine
  }

  it should "work with userhash" in {
    val wwwAuthenticateHeader = """Digest
                                  |realm="api@example.org",
                                  |qop="auth",
                                  |algorithm=SHA-512-256,
                                  |nonce="5TsQWLVdgBdmrQ0XsxbDODV+57QdFR34I9HAbC/RVvkK",
                                  |opaque="HRPCssKJSGjCrkzDg8OhwpzCiGPChXYjwrI2QmXDnsOS",
                                  |charset=UTF-8,
                                  |userhash=true""".oneLine

    val uri = Uri.create("http://api.example.org/doe.json")
    val challenge = DigestAuth.parseWwwAuthenticateHeader(wwwAuthenticateHeader, uri).succeeded
    val cnonce = "NTg6RKcb9boFIAS3KrFK9BGeh+iDa/sm6jUMp2wds69v"
    DigestAuth.generateAuthorization0(
      challenge = challenge,
      username = "J\u00E4s\u00F8n Doe",
      password = "Secret, or not?",
      requestMethod = HttpMethod.GET,
      requestUri = uri,
      nc = 1,
      cnonce = cnonce
    ) shouldBe s"""Digest
                  |username="793263caabb707a56211940d90411ea4a575adeccb7e360aeb624ed06ece9b0b",
                  |realm="api@example.org",
                  |uri="/doe.json",
                  |algorithm=SHA-512-256,
                  |nonce="5TsQWLVdgBdmrQ0XsxbDODV+57QdFR34I9HAbC/RVvkK",
                  |nc=00000001,
                  |cnonce="$cnonce",
                  |qop=auth,
                  |response="3798d4131c277846293534c3edc11bd8a5e4cdcbff78b05db9d95eeb1cec68a5",
                  |opaque="HRPCssKJSGjCrkzDg8OhwpzCiGPChXYjwrI2QmXDnsOS",
                  |userhash=true""".stripMargin.oneLine
  }
}
