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

import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.mock.MockitoSugar

import com.ning.http.client.uri.UriComponents

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.cache.PermanentRedirect
import io.gatling.http.MockUtils

class HttpRequestActionSpec extends FlatSpec with Matchers with MockitoSugar {

  GatlingConfiguration.setUpForTest()

  trait Context {
    val httpEngineMock = mock[HttpEngine]
    var session = Session("mockSession", "mockUserName")

    def addRedirect(from: String, to: String): Unit =
      session = PermanentRedirect.addRedirect(session, UriComponents.create(from), UriComponents.create(to))
  }

  "HttpRequestAction" should "send same transaction with no redirect" in new Context {
    val tx = MockUtils.txTo("http://example.com/", session)
    HttpRequestAction.startHttpTransaction(tx, httpEngineMock)(null)
    verify(httpEngineMock, times(1)).startHttpTransaction(tx)
  }

  it should "update transaction in case of a redirect" in new Context {
    addRedirect("http://example.com/", "http://gatling-tool.org/")
    val tx = MockUtils.txTo("http://example.com/", session)
    HttpRequestAction.startHttpTransaction(tx, httpEngineMock)(null)

    val argumentCapture = ArgumentCaptor.forClass(classOf[HttpTx])
    verify(httpEngineMock, times(1)).startHttpTransaction(argumentCapture.capture())
    val actualTx = argumentCapture.getValue

    actualTx.request.ahcRequest.getURI shouldBe UriComponents.create("http://gatling-tool.org/")
    actualTx.redirectCount shouldBe 1
  }
}

