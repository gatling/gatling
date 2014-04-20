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
package io.gatling.http.ahc

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.specs2.mutable.{ Around, Specification }
import io.gatling.http.action.{ HttpRequestActionExecutor, HttpRequestAction }
import org.mockito.Mockito._
import org.mockito.Matchers
import org.mockito.Matchers._
import org.specs2.execute.AsResult
import io.gatling.http.config.{ HttpProtocolRequestPart, HttpProtocol }
import io.gatling.core.session.Session
import io.gatling.http.response.{ ResponseBody, HttpResponse, Response }
import com.ning.http.client.{ HttpResponseStatus, Request, FluentCaseInsensitiveStringsMap }
import java.net.URI
import akka.actor.ActorContext
import io.gatling.http.HeaderNames
import java.util
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.akka.GatlingActorSystem
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class AsyncHandlerActorTest extends Specification {
  sequential

  step {
    GatlingConfiguration.setUp()
  }

  step {
    GatlingActorSystem.start()
  }

  step {
    AsyncHandlerActor.start()
  }

  trait HttpActionExecutorMockContext extends Around {
    var httpRequestActionExectuor = HttpRequestAction.instance
    var executorMock = mock(classOf[HttpRequestActionExecutor])

    def around[T: AsResult](t: => T) = {
      HttpRequestAction.instance = executorMock
      val result = AsResult(t)
      HttpRequestAction.instance = httpRequestActionExectuor
      result
    }

    def httpTxTo(uri: String, redirectCount: Int = 0): HttpTx = {

      val protocol = mock(classOf[HttpProtocol])
      val session = Session("mockScenario", "mockUser")
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
        true,
        Seq(),
        None,
        false,
        redirectCount)

      when(protocol.requestPart).thenReturn(requestPart)
      when(requestPart.cache).thenReturn(false)

      when(request.getURI).thenReturn(new URI(uri))
      when(request.getHeaders).thenReturn(new FluentCaseInsensitiveStringsMap())

      tx
    }

    def redirectResponse(statusCode: Int) = {
      val responseStatus = mock(classOf[HttpResponseStatus])
      when(responseStatus.getStatusCode).thenReturn(statusCode)

      val headers = new FluentCaseInsensitiveStringsMap()
      headers.put(HeaderNames.LOCATION, util.Arrays.asList("http://galing-tool.org/"))

      HttpResponse(
        mock(classOf[Request]),
        Some(responseStatus),
        headers,
        mock(classOf[ResponseBody]),
        Map[String, String](),
        0,
        null,
        0, 0, 0, 0)
    }

  }

  "async handler actor" should {
    "use usual transaction in case of temporary redirect" in new HttpActionExecutorMockContext {
      println("temporary redirect")
      val temporaryRedirect = redirectResponse(303)
      val tx = httpTxTo("http://example.com/")

      AsyncHandlerActor.instance ! OnCompleted(tx, temporaryRedirect)
      // FIXME This is awful
      Thread.sleep(200)

      verify(executorMock).startHttpTransaction(any(classOf[HttpTx]))(any(classOf[ActorContext]))
    }

    "report redirect in case of permanent redirect" in new HttpActionExecutorMockContext {
      println("permanent redirect")
      val permanentRedirect = redirectResponse(301)
      val tx = httpTxTo("http://example.com/")

      AsyncHandlerActor.instance ! OnCompleted(tx, permanentRedirect)
      // FIXME This is awful
      Thread.sleep(200)

      verify(executorMock).httpTransactionRedirect(Matchers.eq(new URI("http://example.com/")), any(classOf[HttpTx]))(any(classOf[ActorContext]))
    }
  }
}
