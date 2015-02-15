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

import com.ning.http.client.Request
import com.ning.http.client.uri.Uri
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.cache.HttpCaches
import io.gatling.http.config.{ DefaultHttpProtocol, HttpProtocol, HttpProtocolRequestPart }
import io.gatling.http.fetch.ResourceFetcher
import io.gatling.http.request.{ HttpRequest, HttpRequestConfig }
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }

class HttpTxSpec extends FlatSpec with Matchers with MockitoSugar {

  implicit val configuration = GatlingConfiguration.loadForTest()

  trait Context {
    implicit val httpEngineMock = mock[HttpEngine]
    var session = Session("mockSession", "mockUserName")
    implicit val httpCaches = new HttpCaches
    implicit val resourceFetcher = mock[ResourceFetcher]

    val configBase = HttpRequestConfig(
      checks = Nil,
      responseTransformer = None,
      extraInfoExtractor = None,
      maxRedirects = None,
      throttled = false,
      silent = None,
      followRedirect = false,
      discardResponseChunks = true,
      protocol = new DefaultHttpProtocol().value,
      explicitResources = Nil)

    def addRedirect(from: String, to: String): Unit =
      session = httpCaches.addRedirect(session, Uri.create(from), Uri.create(to))
  }

  def tx(ahcRequest: Request, config: HttpRequestConfig, primary: Boolean) =
    HttpTx(null,
      request = HttpRequest(
        requestName = "mockHttpTx",
        ahcRequest = ahcRequest,
        config = config),
      responseBuilderFactory = null,
      primary = primary,
      next = null,
      redirectCount = 0)

  "HttpTx" should "be silent when using default protocol and containing a request forced to silent" in new Context {

    val ahcRequest = mock[Request]

    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/")

    val config = configBase.copy(silent = Some(true))
    tx(ahcRequest, config, primary = true).silent shouldBe true
  }

  it should "be non-silent when using default protocol and containing a regular request" in new Context {

    val ahcRequest = mock[Request]

    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/")

    val config = configBase.copy(silent = None)
    tx(ahcRequest, config, primary = true).silent shouldBe false
  }

  it should "not be silent when using a protocol with a silentURI pattern match the request url" in new Context {

    val ahcRequest = mock[Request]
    val protocol = mock[HttpProtocol]
    val requestPart = mock[HttpProtocolRequestPart]

    when(ahcRequest.getUrl) thenReturn "http://example.com/test.js"
    when(requestPart.silentURI) thenReturn Some(""".*\.js""".r.pattern)
    when(requestPart.silentResources) thenReturn false
    when(protocol.requestPart) thenReturn requestPart

    val config = configBase.copy(silent = None, protocol = protocol)
    tx(ahcRequest, config, primary = true).silent shouldBe true
  }

  it should "be silent when passed a protocol silencing resources and a resource (non primary) request" in new Context {

    val ahcRequest = mock[Request]
    val protocol = mock[HttpProtocol]
    val requestPart = mock[HttpProtocolRequestPart]

    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/test.js")
    when(requestPart.silentURI) thenReturn None
    when(requestPart.silentResources) thenReturn true
    when(protocol.requestPart) thenReturn requestPart

    val config = configBase.copy(silent = None, protocol = protocol)
    tx(ahcRequest, config, primary = false).silent shouldBe true
  }

  it should "not be silent when passed a protocol silencing resources and a primary request" in new Context {

    val ahcRequest = mock[Request]
    val protocol = mock[HttpProtocol]
    val requestPart = mock[HttpProtocolRequestPart]

    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/test.js")
    when(requestPart.silentURI) thenReturn None
    when(requestPart.silentResources) thenReturn true
    when(protocol.requestPart) thenReturn requestPart

    val config = configBase.copy(silent = None, protocol = protocol)
    tx(ahcRequest, config, primary = true).silent shouldBe false
  }
}
