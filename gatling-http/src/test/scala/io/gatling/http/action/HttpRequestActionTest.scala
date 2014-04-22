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
package io.gatling.http.action

import org.junit.runner.RunWith

import org.specs2.mutable.{ Before, Around, Specification }
import org.specs2.runner.JUnitRunner

import org.mockito.Mockito._
import org.mockito.Matchers
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.config.{ HttpProtocolRequestPart, HttpProtocol }
import io.gatling.http.request.HttpRequest
import io.gatling.core.session.Session
import com.ning.http.client.Request
import java.net.URI
import org.specs2.execute.AsResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.akka.GatlingActorSystem
import org.mockito.ArgumentMatcher
import org.specs2.mock.mockito.CalledMatchers

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class HttpRequestActionTest extends Specification with CalledMatchers {
  sequential

  var httpEngine: Option[HttpEngine] = _
  val httpRequestExecutor = new HttpRequestActionExecutorImpl
  step {
    GatlingConfiguration.setUp()
  }

  step {
    GatlingActorSystem.start()
  }

  step {
    httpEngine = HttpEngine._instance
  }

  trait HttpEngineMockContext extends Before {

    var httpEngineMock: HttpEngine = _

    def before() {
      httpEngineMock = mock(classOf[HttpEngine])
      HttpEngine._instance = Some(httpEngineMock)
    }

    def httpTxTo(uri: String, redirectCount: Int = 0): HttpTx = {

      val protocol = mock(classOf[HttpProtocol])
      val session = mock(classOf[Session])
      val request = mock(classOf[Request])
      val requestPart = mock(classOf[HttpProtocolRequestPart])

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

      when(protocol.requestPart).thenReturn(requestPart)
      when(requestPart.cache).thenReturn(false)

      when(request.getURI).thenReturn(new URI(uri))

      tx
    }

    def addRedirect(from: String, to: String) {
      val tx = httpTxTo(to)
      httpRequestExecutor.httpTransactionRedirect(new URI(from), tx)(null)
      reset(httpEngineMock)
    }
  }

  def stubHttpTx() =
    HttpTx(null, null, "name", List(), null, null, null, false, Some(1), false, false, Seq[HttpRequest](), None)

  class HttpTxMatcher(expectedURI: URI, expectedRedirectCount: Int) extends ArgumentMatcher[HttpTx] {
    def matches(argument: AnyRef): Boolean = {
      argument match {
        case httpTx: HttpTx => httpTx.request.getURI == expectedURI &&
          httpTx.redirectCount == expectedRedirectCount

        case _ => false
      }
    }
  }

  "http request action" should {

    "call HttpEngine on request" in new HttpEngineMockContext {
      val tx = httpTxTo("http://example.com")
      httpRequestExecutor.startHttpTransaction(tx)(null)
      verify(httpEngineMock).startHttpTransaction(tx)
    }

    "call HttpEngine on redirect" in new HttpEngineMockContext {
      val tx = httpTxTo("http://gatling-tool.org")
      httpRequestExecutor.httpTransactionRedirect(new URI("http://example.com"), tx)(null)
      verify(httpEngineMock).startHttpTransaction(tx)
    }

    "memomize redirect" in new HttpEngineMockContext {
      addRedirect("http://example.com/", "http://gatling-tool.org/")

      httpRequestExecutor.startHttpTransaction(httpTxTo("http://example.com/"))(null)
      verify(httpEngineMock).startHttpTransaction(
        Matchers.argThat(new HttpTxMatcher(new URI("http://gatling-tool.org/"), 1)))
    }

    "skip several redirects" in new HttpEngineMockContext {
      addRedirect("http://example.com/", "http://gatling-tool.org/")
      addRedirect("http://gatling-tool.org/", "http://gatling2-tool.org/")
      addRedirect("http://gatling2-tool.org/", "http://gatling3-tool.org/")

      httpRequestExecutor.startHttpTransaction(httpTxTo("http://example.com/"))(null)
      verify(httpEngineMock).startHttpTransaction(
        Matchers.argThat(new HttpTxMatcher(new URI("http://gatling3-tool.org/"), 3)))
    }

    "skip several redirects after redirect" in new HttpEngineMockContext {
      addRedirect("http://example.com/", "http://gatling-tool.org/")
      addRedirect("http://gatling-tool.org/", "http://gatling2-tool.org/")
      addRedirect("http://gatling2-tool.org/", "http://gatling3-tool.org/")

      // redirectCount is already 1
      httpRequestExecutor.startHttpTransaction(httpTxTo("http://example.com/", 1))(null)

      // redirectCount should be increased
      verify(httpEngineMock).startHttpTransaction(
        Matchers.argThat(new HttpTxMatcher(new URI("http://gatling3-tool.org/"), 4)))
    }
  }

  step {
    HttpEngine._instance = httpEngine
  }

}
