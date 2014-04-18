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
import org.specs2.mock.mockito.CalledMatchers
import org.specs2.runner.JUnitRunner

import org.mockito.Mockito._
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.config.{ HttpProtocolRequestPart, HttpProtocol }
import io.gatling.http.request.HttpRequest
import io.gatling.core.session.Session
import com.ning.http.client.Request
import java.net.URI
import org.specs2.execute.AsResult

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class HttpRequestActionTest extends Specification with CalledMatchers {

  var httpEngine: Option[HttpEngine] = _

  step {
    httpEngine = HttpEngine._instance
  }

  trait HttpActionExecutorMockContext extends Around {
    val httpRequestActionExectuor = HttpRequestAction.instance
    val executorMock = mock(classOf[HttpRequestActionExecutor])

    def around[T: AsResult](t: => T) = {
      HttpRequestAction.instance = executorMock
      val result = AsResult(t)
      HttpRequestAction.instance = httpRequestActionExectuor
      result
    }
  }

  trait HttpEngineMockContext extends Before {

    val httpEngineMock: HttpEngine = mock(classOf[HttpEngine])

    def before() {
      HttpEngine._instance = Some(httpEngineMock)
    }
  }

  def stubHttpTx() =
    HttpTx(null, null, "name", List(), null, null, null, false, Some(1), false, false, Seq[HttpRequest](), None)

  "http request action" should {
    "call request executor instance" in new HttpActionExecutorMockContext {
      val tx = stubHttpTx
      HttpRequestAction.startHttpTransaction(tx)(null)

      verify(executorMock).startHttpTransaction(tx)(null)
    }

    "call HttpEngine on request" in new HttpEngineMockContext {
      val tx = mock(classOf[HttpTx])
      val protocol = mock(classOf[HttpProtocol])
      val session = mock(classOf[Session])
      val request = mock(classOf[Request])
      val requestPart = mock(classOf[HttpProtocolRequestPart])

      when(tx.session).thenReturn(session)
      when(tx.protocol).thenReturn(protocol)
      when(tx.request).thenReturn(request)
      when(protocol.requestPart).thenReturn(requestPart)
      when(requestPart.cache).thenReturn(false)

      when(request.getURI).thenReturn(new URI("http://example.com"))

      HttpRequestAction.startHttpTransaction(tx)(null)

      verify(httpEngineMock).startHttpTransaction(tx)
    }
  }

  step {
    HttpEngine._instance = httpEngine
  }

}
