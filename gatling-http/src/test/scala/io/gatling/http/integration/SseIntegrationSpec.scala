/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.nio.charset.StandardCharsets.UTF_8

import scala.concurrent.duration._

import io.gatling.core.CoreDsl
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.{ HttpDsl, HttpSpec }

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.{
  DefaultHttpContent,
  DefaultHttpResponse,
  HttpHeaderNames => NettyHeaderNames,
  HttpHeaderValues => NettyHeaderValues,
  HttpResponseStatus,
  HttpVersion
}

class SseIntegrationSpec extends HttpSpec with CoreDsl with HttpDsl {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  // sends the events right away and keeps the stream open
  private def streamEvents(events: String*): Handler = { case _ =>
    ctx => {
      val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
      response.headers
        .set(NettyHeaderNames.CONTENT_TYPE, "text/event-stream")
        .set(NettyHeaderNames.TRANSFER_ENCODING, NettyHeaderValues.CHUNKED)
      ctx.write(response)
      ctx.writeAndFlush(new DefaultHttpContent(Unpooled.copiedBuffer(events.map(data => s"data: $data\n\n").mkString, UTF_8)))
    }
  }

  "setCheck" should "match events received before the check was set, keeping the non-matching ones for later setChecks" in {
    runWithHttpServer(streamEvents("one", "two")) { _ =>
      val session = runScenario(
        scenario("sse").exec(
          // no check: events are received while idle
          sse("connect").get("/sse"),
          pause(500.millis),
          sse("second").setCheck.await(1.second)(sse.checkMessage("second").matching(jsonPath("$.data").is("two"))),
          sse("first").setCheck.await(1.second)(sse.checkMessage("first").matching(jsonPath("$.data").is("one"))),
          sse("close").close
        ),
        protocolCustomizer = _.sseSetCheckBuffer(10, 5.seconds)
      )

      session.isFailed shouldBe false
    }
  }

  it should "match events that didn't match a previous check" in {
    runWithHttpServer(streamEvents("one", "two")) { _ =>
      val session = runScenario(
        scenario("sse").exec(
          // "one" arrives while checking for "two"
          sse("connect").get("/sse").await(1.second)(sse.checkMessage("second").matching(jsonPath("$.data").is("two"))),
          sse("first").setCheck.await(1.second)(sse.checkMessage("first").matching(jsonPath("$.data").is("one"))),
          sse("close").close
        ),
        protocolCustomizer = _.sseSetCheckBuffer(10, 5.seconds)
      )

      session.isFailed shouldBe false
    }
  }

  it should "retry the events that didn't match a check against the next checks of the sequence" in {
    runWithHttpServer(streamEvents("one", "two")) { _ =>
      val session = runScenario(
        scenario("sse").exec(
          sse("connect").get("/sse"),
          pause(500.millis),
          // "one" doesn't match the first check but matches the second one
          sse("both").setCheck.await(1.second)(
            sse.checkMessage("second").matching(jsonPath("$.data").is("two")),
            sse.checkMessage("first").matching(jsonPath("$.data").is("one"))
          ),
          sse("close").close
        ),
        protocolCustomizer = _.sseSetCheckBuffer(10, 5.seconds)
      )

      session.isFailed shouldBe false
    }
  }

  it should "not match events received before the check was set by default" in {
    runWithHttpServer(streamEvents("one")) { _ =>
      val session = runScenario(
        scenario("sse").exec(
          sse("connect").get("/sse"),
          pause(500.millis),
          sse("first").setCheck.await(1.second)(sse.checkMessage("first").check(jsonPath("$.data").is("one"))),
          sse("close").close
        )
      )

      session.isFailed shouldBe true
    }
  }

  it should "not match events older than the buffer max age" in {
    runWithHttpServer(streamEvents("one")) { _ =>
      val session = runScenario(
        scenario("sse").exec(
          sse("connect").get("/sse"),
          pause(500.millis),
          sse("first").setCheck.await(1.second)(sse.checkMessage("first").check(jsonPath("$.data").is("one"))),
          sse("close").close
        ),
        protocolCustomizer = _.sseSetCheckBuffer(10, 100.millis)
      )

      session.isFailed shouldBe true
    }
  }
}
