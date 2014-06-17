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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.config.{ HttpProtocolBuilder, HttpProtocol }
import io.gatling.core.session._
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.cache.PermanentRedirect

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.{ Before, Specification }
import org.specs2.mock.Mockito

import com.ning.http.client.Request

import java.net.URI

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class HttpRequestDefSpec extends Specification with Mockito {

  GatlingConfiguration.setUp()

  trait Context extends Before {
    val httpEngineMock = mock[HttpEngine]
    var session = Session("mockSession", "mockUserName")

    def addRedirect(from: String, to: String) {
      session = PermanentRedirect.addRedirect(session, new URI(from), new URI(to))
    }

    def before() {}
  }

  "HttpRequestDef" should {

    "build silent HttpRequest when being silent" in new Context {
      val config = HttpRequestConfig(
        checks = Nil,
        responseTransformer = None,
        extraInfoExtractor = None,
        maxRedirects = None,
        throttled = false,
        silent = true, // here
        followRedirect = false,
        protocol = HttpProtocol.DefaultHttpProtocol,
        explicitResources = Nil)

      val ahcRequest = mock[Request]
      ahcRequest.getURI returns new URI("http://example.com/")

      val httpRequestDef = HttpRequestDef("foo".expression, ahcRequest.expression, None, config)
      val httpRequest = httpRequestDef.build(session)

      httpRequest.get.config.silent should beTrue
    }

    "build non-silent HttpRequest when being non-silent" in new Context {
      val config = HttpRequestConfig(
        checks = Nil,
        responseTransformer = None,
        extraInfoExtractor = None,
        maxRedirects = None,
        throttled = false,
        silent = false, // here
        followRedirect = false,
        protocol = HttpProtocol.DefaultHttpProtocol,
        explicitResources = Nil)

      val ahcRequest = mock[Request]
      ahcRequest.getURI returns new URI("http://example.com/")

      val httpRequestDef = HttpRequestDef("foo".expression, ahcRequest.expression, None, config)
      val httpRequest = httpRequestDef.build(session)

      httpRequest.get.config.silent should beFalse
    }

    "build non-silent HttpRequest when passed a non-silent protocol" in new Context {
      val ahcRequest = mock[Request]
      ahcRequest.getURI returns new URI("http://example.com/test.js")

      val protocol = new HttpProtocolBuilder(HttpProtocol.DefaultHttpProtocol).silentURI(".*js")
      val config = HttpRequestConfig(
        checks = Nil,
        responseTransformer = None,
        extraInfoExtractor = None,
        maxRedirects = None,
        throttled = false,
        silent = false, // here
        followRedirect = false,
        protocol = protocol, // here
        explicitResources = Nil)

      val httpRequestDef = HttpRequestDef("foo".expression, ahcRequest.expression, None, config)
      val httpRequest = httpRequestDef.build(session)

      httpRequest.get.config.silent should beTrue
    }
  }
}
