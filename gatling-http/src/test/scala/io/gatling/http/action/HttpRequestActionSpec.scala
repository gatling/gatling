/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action

import io.gatling.http.action.HttpRequestAction
import io.gatling.http.request.HttpRequest
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.{ Before, Specification }
import org.specs2.mock.Mockito
import io.gatling.core.session.Session
import io.gatling.http.config.{ HttpProtocolBuilder, HttpProtocolRequestPart, HttpProtocol }
import com.ning.http.client.Request
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import java.net.URI
import org.mockito.ArgumentMatcher
import akka.actor.ActorContext
import org.specs2.matcher.{ MatchResult, Expectable }
import org.specs2.mock.mockito.ArgumentCapture

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class HttpRequestActionSpec extends Specification with Mockito {

  trait Context extends Before {
    val httpEngineMock = mock[HttpEngine]
    var session = Session("mockSession", "mockUserName")

    def txTo(uri: String, session: Session, redirectCount: Int = 0) = {
      val protocol = mock[HttpProtocol]
      val request = mock[Request]
      val requestPart = mock[HttpProtocolRequestPart]

      val tx = HttpTx(session,
        request,
        "mockHttpTx",
        List(),
        null,
        protocol,
        null,
        true,
        Some(10),
        false,
        false,
        Seq(),
        None,
        false,
        redirectCount)

      protocol.requestPart returns requestPart
      requestPart.cache returns false

      request.getURI returns (new URI(uri))

      tx
    }

    def before() {}
  }

  "http request action" should {
    "silent requests should remain silent" in {
      val request = mock[HttpRequest]
      val ahcRequest = mock[Request]
      request.protocol returns HttpProtocol.default
      request.silent returns true
      ahcRequest.getURI returns new URI("http://example.com/")

      HttpRequestAction.isSilent(ahcRequest, request) should beTrue
    }

    "silent requests should remain silent" in {
      val request = mock[HttpRequest]
      val ahcRequest = mock[Request]
      request.silent returns true
      request.protocol returns HttpProtocol.default
      ahcRequest.getURI returns new URI("http://example.com/")

      HttpRequestAction.isSilent(ahcRequest, request) should beTrue
    }

    "non-silent requests with default protocol should remain non-silent" in {
      val request = mock[HttpRequest]
      val ahcRequest = mock[Request]

      request.silent returns false
      request.protocol returns HttpProtocol.default
      ahcRequest.getURI returns new URI("http://example.com/")

      HttpRequestAction.isSilent(ahcRequest, request) should beFalse
    }

    "non-silent requests with default protocol should remain non-silent" in {
      val request = mock[HttpRequest]
      val ahcRequest = mock[Request]
      request.silent returns false
      ahcRequest.getURI returns new URI("http://example.com/test.js")

      val protocol = new HttpProtocolBuilder(HttpProtocol.default).silentURI(".*js")
      request.protocol returns protocol

      HttpRequestAction.isSilent(ahcRequest, request) should beTrue
    }
  }
}