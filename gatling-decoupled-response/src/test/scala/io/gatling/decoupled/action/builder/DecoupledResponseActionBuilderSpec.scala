/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.decoupled.action.builder

import java.nio.charset.StandardCharsets
import java.util.UUID

import io.gatling.core.CoreDsl
import io.gatling.core.config.GatlingConfiguration
import io.netty.handler.codec.http.{ DefaultFullHttpResponse, FullHttpRequest, HttpResponseStatus, HttpVersion }
import io.gatling.decoupled.DecoupledResponseDsl
import io.gatling.http.{ HttpDsl, HttpSpec }
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener

import scala.util.{ Success, Try }

class DecoupledResponseActionBuilderSpec extends HttpSpec with CoreDsl with HttpDsl with DecoupledResponseDsl {

  ignore should "prepare http request with UUID header" in {

    runWithHttpServer(catchAllHandler) { implicit httpServer =>
      val session = runScenario(
        scenario("Cookie Redirect")
          .exec(
            decoupledResponse("testreq", http("fire").get(testPath))
              .correlationIdHeaderName(correlationHeaderName)
          ),
        protocolCustomizer = _.inferHtmlResources()
      )

      session.isFailed shouldBe false
      verifyRequestTo(testPath, 1, checkCorrelationHeader(correlationHeaderName))

    }

  }

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  val correlationHeaderName = "x-test-header"
  val testPath = "/"

  def checkCorrelationHeader(header: String)(request: FullHttpRequest): Unit = {
    val headerValue = Option(request.headers.get(header))

    headerValue shouldBe defined

    Try {
      UUID.fromString(headerValue.get)
    } shouldBe a[Success[_]]

  }

  val catchAllHandler: Handler = {
    case _ =>
      val bytes = "Hello".getBytes(StandardCharsets.UTF_8)
      val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(bytes))
      ctx => ctx.channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
  }

}
