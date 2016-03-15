/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.ahc

import io.gatling.BaseSpec
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.action.sync.HttpTx
import io.gatling.http.cache.HttpCaches
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol, HttpProtocolRequestPart }
import io.gatling.http.request.{ HttpRequest, HttpRequestConfig }

import akka.actor.ActorRef
import org.asynchttpclient.Request
import org.asynchttpclient.uri.Uri
import org.mockito.Mockito._

class HttpTxSpec extends BaseSpec {

  implicit val configuration = GatlingConfiguration.loadForTest()

  trait Context {
    val httpCaches = new HttpCaches(configuration)
    val coreComponents = mock[CoreComponents]
    when(coreComponents.configuration).thenReturn(configuration)
    val httpComponents = HttpComponents(HttpProtocol(configuration), mock[HttpEngine], httpCaches, mock[ResponseProcessor])
    var session = Session("mockSession", 0)

    val configBase = HttpRequestConfig(
      checks = Nil,
      responseTransformer = None,
      extraInfoExtractor = None,
      maxRedirects = None,
      throttled = false,
      silent = None,
      followRedirect = false,
      discardResponseChunks = true,
      coreComponents = coreComponents,
      httpComponents = httpComponents,
      explicitResources = Nil
    )
  }

  def tx(ahcRequest: Request, config: HttpRequestConfig, root: Boolean) =
    HttpTx(
      null,
      request = HttpRequest(
        requestName = "mockHttpTx",
        ahcRequest = ahcRequest,
        config = config
      ),
      responseBuilderFactory = null,
      next = mock[Action],
      resourceFetcher = if (root) None else Some(mock[ActorRef]),
      redirectCount = 0
    )

  "HttpTx" should "be silent when using default protocol and containing a request forced to silent" in new Context {

    val ahcRequest = mock[Request]

    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/")

    val config = configBase.copy(silent = Some(true))
    tx(ahcRequest, config, root = true).silent shouldBe true
  }

  it should "be non-silent when using default protocol and containing a regular request" in new Context {

    val ahcRequest = mock[Request]

    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/")

    val config = configBase.copy(silent = None)
    tx(ahcRequest, config, root = true).silent shouldBe false
  }

  it should "not be silent when using a protocol with a silentURI pattern match the request url" in new Context {

    val ahcRequest = mock[Request]
    val httpProtocol = mock[HttpProtocol]
    val requestPart = mock[HttpProtocolRequestPart]

    when(ahcRequest.getUrl) thenReturn "http://example.com/test.js"
    when(requestPart.silentURI) thenReturn Some(""".*\.js""".r.pattern)
    when(requestPart.silentResources) thenReturn false
    when(httpProtocol.requestPart) thenReturn requestPart

    val config = configBase.copy(silent = None, httpComponents = httpComponents.copy(httpProtocol = httpProtocol))
    tx(ahcRequest, config, root = true).silent shouldBe true
  }

  it should "be silent when passed a protocol silencing resources and a resource (non root) request" in new Context {

    val ahcRequest = mock[Request]
    val httpProtocol = mock[HttpProtocol]
    val requestPart = mock[HttpProtocolRequestPart]

    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/test.js")
    when(requestPart.silentURI) thenReturn None
    when(requestPart.silentResources) thenReturn true
    when(httpProtocol.requestPart) thenReturn requestPart

    val config = configBase.copy(silent = None, httpComponents = httpComponents.copy(httpProtocol = httpProtocol))
    tx(ahcRequest, config, root = false).silent shouldBe true
  }

  it should "not be silent when passed a protocol silencing resources and a root request" in new Context {

    val ahcRequest = mock[Request]
    val httpProtocol = mock[HttpProtocol]
    val requestPart = mock[HttpProtocolRequestPart]

    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/test.js")
    when(requestPart.silentURI) thenReturn None
    when(requestPart.silentResources) thenReturn true
    when(httpProtocol.requestPart) thenReturn requestPart

    val config = configBase.copy(silent = None, httpComponents = httpComponents.copy(httpProtocol = httpProtocol))
    tx(ahcRequest, config, root = true).silent shouldBe false
  }
}
