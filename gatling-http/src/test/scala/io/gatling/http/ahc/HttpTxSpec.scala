/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.http.action.sync.HttpTx
import io.gatling.http.cache.HttpCaches
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol }
import io.gatling.http.request.{ HttpRequest, HttpRequestConfig }

import akka.actor.ActorRef
import com.softwaremill.quicklens._
import org.asynchttpclient.Request
import org.asynchttpclient.uri.Uri
import org.mockito.Mockito._

class HttpTxSpec extends BaseSpec {

  implicit val configuration = GatlingConfiguration.loadForTest()

  trait Context {
    val coreComponents = mock[CoreComponents]
    when(coreComponents.configuration).thenReturn(configuration)
    val httpProtocol = HttpProtocol(configuration)
    val httpComponents = HttpComponents(httpProtocol, mock[HttpEngine], new HttpCaches(configuration), mock[ResponseProcessor])

    val configBase = HttpRequestConfig(
      checks = Nil,
      responseTransformer = None,
      extraInfoExtractor = None,
      maxRedirects = 20,
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
      resourceFetcher = if (root) None else Some(mock[ActorRef])
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

    tx(ahcRequest, configBase, root = true).silent shouldBe false
  }

  it should "not be silent when using a protocol with a silentURI pattern match the request url" in new Context {

    val ahcRequest = mock[Request]
    when(ahcRequest.getUrl) thenReturn "http://example.com/test.js"

    val config = configBase
      .modify(_.httpComponents.httpProtocol.requestPart)
      .using(_.modify(_.silentURI).setTo(Some(""".*\.js""".r.pattern)).modify(_.silentResources).setTo(false))

    tx(ahcRequest, config, root = true).silent shouldBe true
  }

  it should "be silent when passed a protocol silencing resources and a resource (non root) request" in new Context {

    val ahcRequest = mock[Request]
    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/test.js")

    val config = configBase
      .modify(_.httpComponents.httpProtocol.requestPart)
      .using(_.modify(_.silentURI).setTo(None).modify(_.silentResources).setTo(true))

    tx(ahcRequest, config, root = false).silent shouldBe true
  }

  it should "not be silent when passed a protocol silencing resources and a root request" in new Context {

    val ahcRequest = mock[Request]
    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/test.js")

    val config = configBase
      .modify(_.httpComponents.httpProtocol.requestPart)
      .using(_.modify(_.silentURI).setTo(None).modify(_.silentResources).setTo(true))

    tx(ahcRequest, config, root = true).silent shouldBe false
  }
}
