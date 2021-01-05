/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.integration

import java.nio.charset.StandardCharsets

import io.gatling.core.CoreDsl
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.HttpSpec

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.http.{ DefaultFullHttpResponse, HttpHeaderNames => NettyHttpHeaderName, HttpMethod, HttpResponseStatus, HttpVersion }
import io.netty.handler.codec.http.cookie._

class HttpIntegrationSpec extends HttpSpec with CoreDsl with HttpDsl {

  private val regexCheck = super[CoreDsl].regex(_)

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  ignore should "send cookies returned in redirects in subsequent requests" in {

    val handler: Handler = {
      case HttpRequest(HttpMethod.GET, "/page1") =>
        val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY)
        response
          .headers()
          .set(NettyHttpHeaderName.SET_COOKIE, ServerCookieEncoder.STRICT.encode(new DefaultCookie("TestCookie1", "Test1")))
          .set(NettyHttpHeaderName.LOCATION, "/page2")
          .set(NettyHttpHeaderName.CONTENT_LENGTH, 0)

        ctx => ctx.channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)

      case HttpRequest(HttpMethod.GET, "/page2") =>
        val bytes = "Hello World".getBytes(StandardCharsets.UTF_8)

        val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(bytes))
        response
          .headers()
          .set(NettyHttpHeaderName.SET_COOKIE, ServerCookieEncoder.STRICT.encode(new DefaultCookie("TestCookie2", "Test2")))
          .set(NettyHttpHeaderName.CONTENT_LENGTH, bytes.length)

        ctx => ctx.channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)

      case HttpRequest(HttpMethod.GET, "/page3") =>
        val bytes = "Hello Again".getBytes(StandardCharsets.UTF_8)

        val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(bytes))
        response
          .headers()
          .set(NettyHttpHeaderName.SET_COOKIE, ServerCookieEncoder.STRICT.encode(new DefaultCookie("TestCookie2", "Test2")))
          .set(NettyHttpHeaderName.CONTENT_LENGTH, bytes.length)

        ctx => ctx.channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

    runWithHttpServer(handler) { implicit httpServer =>
      val session = runScenario(
        scenario("Cookie Redirect")
          .exec(
            http("/page1")
              .get("/page1")
              .check(
                regexCheck("Hello World"),
                currentLocation.is(s"http://localhost:$mockHttpPort/page2")
              )
          )
          .exec(
            http("/page3")
              .get("/page3")
              .check(
                regexCheck("Hello Again")
              )
          )
      )

      session.isFailed shouldBe false

      verifyRequestTo("/page1")
      verifyRequestTo("/page2", 1, checkCookie("TestCookie1", "Test1"))
      verifyRequestTo("/page3", 1, checkCookie("TestCookie1", "Test1"), checkCookie("TestCookie2", "Test2"))
    }
  }

  ignore should "retrieve linked resources, when resource downloading is enabled" in {

    val handler: Handler = { case HttpRequest(HttpMethod.GET, path) =>
      sendFile(path.drop(1)) // Drop leading slash in path
    }

    runWithHttpServer(handler) { implicit httpServer =>
      val session = runScenario(
        scenario("Resource downloads")
          .exec(
            http("/resourceTest/index.html")
              .get("/resourceTest/index.html")
              .check(
                css("h1").is("Resource Test"),
                regexCheck("<title>Resource Test</title>")
              )
          ),
        protocolCustomizer = _.inferHtmlResources(BlackList(".*/bad_resource.png"))
      )

      session.isFailed shouldBe false

      verifyRequestTo("/resourceTest/index.html")
      verifyRequestTo("/resourceTest/stylesheet.css")
      verifyRequestTo("/resourceTest/script.js")
      verifyRequestTo("/resourceTest/img.png")
      verifyRequestTo("/resourceTest/bad_resource.png", 0)
    }
  }

  ignore should "fetch resources in conditional comments" in {

    val handler: Handler = { case HttpRequest(HttpMethod.GET, path) =>
      sendFile(path.drop(1)) // Drop leading slash in path
    }

    runWithHttpServer(handler) { implicit httpServer =>
      val session = runScenario(
        scenario("Resource downloads")
          .exec(
            http("/resourceTest/indexIE.html")
              .get("/resourceTest/indexIE.html")
              .header(
                "User-Agent",
                "Mozilla/5.0 (Windows; U; MSIE 9.0; WIndows NT 9.0; en-US)"
              )
          ),
        protocolCustomizer = _.inferHtmlResources()
      )

      session.isFailed shouldBe false

      verifyRequestTo("/resourceTest/indexIE.html")
      verifyRequestTo("/resourceTest/stylesheet.css")
    }
  }
}
