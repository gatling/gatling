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

import akka.actor.ActorContext
import io.gatling.http.cache.HttpCaches
import io.gatling.http.fetch.ResourceFetcher
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.mock.MockitoSugar

import com.ning.http.client.uri.Uri

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.MockUtils

class HttpRequestActionSpec extends FlatSpec with Matchers with MockitoSugar {

  implicit val configuration = GatlingConfiguration.loadForTest()
  implicit val httpCaches = new HttpCaches()
  implicit val httpEngineMock = mock[HttpEngine]
  implicit val resourceFetcher = new ResourceFetcher
  implicit val actorContext: ActorContext = null

  trait Context {
    var session = Session("mockSession", "mockUserName")

    def addRedirect(from: String, to: String): Unit =
      session = httpCaches.addRedirect(session, Uri.create(from), Uri.create(to))
  }

  "HttpRequestAction" should "send same transaction with no redirect" in new Context {
    val tx = MockUtils.txTo("http://example.com/", session, cache = true)
    HttpRequestAction.startHttpTransaction(tx)
    verify(httpEngineMock, times(1)).startHttpTransaction(tx)
  }

  it should "update transaction in case of a redirect" in new Context {
    addRedirect("http://example.com/", "http://gatling.io/")
    val tx = MockUtils.txTo("http://example.com/", session, cache = true)
    HttpRequestAction.startHttpTransaction(tx)

    val argumentCapture = ArgumentCaptor.forClass(classOf[HttpTx])
    verify(httpEngineMock, times(2)).startHttpTransaction(argumentCapture.capture())
    val actualTx = argumentCapture.getValue

    actualTx.request.ahcRequest.getUri shouldBe Uri.create("http://gatling.io/")
    actualTx.redirectCount shouldBe 1
  }
}

