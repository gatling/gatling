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

package io.gatling.http.engine

import io.gatling.BaseSpec
import io.gatling.commons.util.DefaultClock
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.action.{ HttpTx, ResourceTx }
import io.gatling.http.cache.HttpCaches
import io.gatling.http.client.Request
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol }
import io.gatling.http.request.{ HttpRequest, HttpRequestConfig }

import akka.actor.ActorSystem
import com.softwaremill.quicklens._
import org.mockito.Mockito._

class HttpTxSpec extends BaseSpec {

  implicit val configuration = GatlingConfiguration.loadForTest()

  trait Context {
    val coreComponents = mock[CoreComponents]
    val clock = new DefaultClock
    when(coreComponents.configuration).thenReturn(configuration)
    when(coreComponents.system).thenReturn(mock[ActorSystem])
    val httpEngine = mock[HttpEngine]
    when(httpEngine.coreComponents).thenReturn(coreComponents)
    val httpProtocol = HttpProtocol(configuration)
    val httpComponents = HttpComponents(httpProtocol, httpEngine, new HttpCaches(clock, configuration), mock[ResponseProcessor], clock)

    val configBase = HttpRequestConfig(
      checks = Nil,
      responseTransformer = None,
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

  def tx(clientRequest: Request, config: HttpRequestConfig, root: Boolean) =
    HttpTx(
      null,
      request = HttpRequest(
        requestName = "mockHttpTx",
        clientRequest = clientRequest,
        config = config
      ),
      responseBuilderFactory = null,
      next = mock[Action],
      resourceTx = if (root) None else Some(mock[ResourceTx])
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
    when(ahcRequest.getUri) thenReturn Uri.create("http://example.com/test.js")

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
