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

import com.ning.http.client.uri.UriComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.cache.PermanentRedirect
import io.gatling.http.MockUtils

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.{ Before, Specification }
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class HttpRequestActionSpec extends Specification with Mockito {

  GatlingConfiguration.setUp()

  trait Context extends Before {
    val httpEngineMock = mock[HttpEngine]
    var session = Session("mockSession", "mockUserName")

    def addRedirect(from: String, to: String): Unit =
      session = PermanentRedirect.addRedirect(session, UriComponents.create(from), UriComponents.create(to))

    def before(): Unit = {}
  }

  "HttpRequestAction" should {
    "send same transaction with no redirect" in new Context {
      val tx = MockUtils.txTo("http://example.com/", session)
      HttpRequestAction.startHttpTransaction(tx, httpEngineMock)(null)
      there was one(httpEngineMock).startHttpTransaction(tx)
    }

    "update transaction in case of a redirect" in new Context {
      addRedirect("http://example.com/", "http://gatling-tool.org/")
      val tx = MockUtils.txTo("http://example.com/", session)
      HttpRequestAction.startHttpTransaction(tx, httpEngineMock)(null)

      val argumentCapture = new ArgumentCapture[HttpTx]
      there was one(httpEngineMock).startHttpTransaction(argumentCapture)
      val actualTx = argumentCapture.value

      actualTx.request.ahcRequest.getURI should be equalTo UriComponents.create("http://gatling-tool.org/")
      actualTx.redirectCount should be equalTo 1
    }
  }
}

