/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.mock.MockitoSugar
import org.scalatest.junit.JUnitRunner

import com.ning.http.client.Request
import com.ning.http.client.uri.UriComponents

import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.config.{ HttpProtocolBuilder, HttpProtocol }
import io.gatling.core.session._
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.cache.PermanentRedirect

@RunWith(classOf[JUnitRunner])
class HttpRequestDefSpec extends FlatSpec with Matchers with MockitoSugar {

  GatlingConfiguration.setUp()

  trait Context {
    val httpEngineMock = mock[HttpEngine]
    var session = Session("mockSession", "mockUserName")
    val configBase = HttpRequestConfig(
      checks = Nil,
      responseTransformer = None,
      extraInfoExtractor = None,
      maxRedirects = None,
      throttled = false,
      silent = false,
      followRedirect = false,
      discardResponseChunks = true,
      protocol = HttpProtocol.DefaultHttpProtocol,
      explicitResources = Nil)

    def addRedirect(from: String, to: String): Unit =
      session = PermanentRedirect.addRedirect(session, UriComponents.create(from), UriComponents.create(to))

  }

  "HttpRequestDef" should "build silent HttpRequest when being silent" in new Context {
    val config = configBase.copy(silent = true)

    val ahcRequest = mock[Request]
    when(ahcRequest.getURI) thenReturn UriComponents.create("http://example.com/")

    val httpRequestDef = HttpRequestDef("foo".expression, ahcRequest.expression, None, config)
    val httpRequest = httpRequestDef.build(session)

    httpRequest.get.config.silent shouldBe true
  }

  it should "build non-silent HttpRequest when being non-silent" in new Context {
    val config = configBase.copy(silent = false)

    val ahcRequest = mock[Request]
    when(ahcRequest.getURI) thenReturn UriComponents.create("http://example.com/")

    val httpRequestDef = HttpRequestDef("foo".expression, ahcRequest.expression, None, config)
    val httpRequest = httpRequestDef.build(session)

    httpRequest.get.config.silent shouldBe false
  }

  it should "build non-silent HttpRequest when passed a non-silent protocol" in new Context {
    val ahcRequest = mock[Request]
    when(ahcRequest.getURI) thenReturn UriComponents.create("http://example.com/test.js")

    val protocol = new HttpProtocolBuilder(HttpProtocol.DefaultHttpProtocol).silentURI(".*js")
    val config = configBase.copy(silent = false, protocol = protocol)

    val httpRequestDef = HttpRequestDef("foo".expression, ahcRequest.expression, None, config)
    val httpRequest = httpRequestDef.build(session)

    httpRequest.get.config.silent shouldBe true
  }
}
