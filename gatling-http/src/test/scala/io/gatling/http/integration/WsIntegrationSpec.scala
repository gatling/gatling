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

import scala.concurrent.duration._

import io.gatling.core.CoreDsl
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.{ HttpDsl, HttpSpec, WebSocketServer }
import io.gatling.http.action.ws.WsInboundMessage

class WsIntegrationSpec extends HttpSpec with CoreDsl with HttpDsl {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private val Path = "/ws"

  private def runWithWebSocketServer(scripted: String => List[String])(f: WebSocketServer => Unit): Unit = {
    val server = new WebSocketServer(mockHttpPort, Path, None, scripted)
    try {
      f(server)
    } finally {
      server.stop()
    }
  }

  private def url = s"ws://localhost:$mockHttpPort$Path"

  "setCheck" should "wait for inbound messages without sending anything" in {
    val handler: String => List[String] = {
      case "go" => List("""{"n":1}""", """{"n":2}""", """{"n":3}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          ws("go").sendText("go").await(10.seconds)(ws.checkTextMessage("first").check(jsonPath("$.n").ofType[Int].is(1))),
          ws("second").setCheck.await(10.seconds)(ws.checkTextMessage("second").check(jsonPath("$.n").ofType[Int].is(2))),
          ws("third").setCheck.await(10.seconds)(ws.checkTextMessage("third").check(jsonPath("$.n").ofType[Int].is(3))),
          ws("close").close
        )
      )

      session.isFailed shouldBe false
    }
  }

  it should "fail when no message arrives before the timeout" in {
    runWithWebSocketServer(_ => Nil) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          ws("nothing").setCheck.await(1.second)(ws.checkTextMessage("never").check(jsonPath("$.n"))),
          ws("close").close
        )
      )

      session.isFailed shouldBe true
    }
  }

  it should "match messages received before the check was set, keeping the non-matching ones for later setChecks" in {
    val handler: String => List[String] = {
      case "go" => List("""{"n":1}""", """{"n":2}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          // no check: replies are received while idle
          ws("go").sendText("go"),
          pause(500.millis),
          ws("second").setCheck.await(1.second)(ws.checkTextMessage("second").matching(jsonPath("$.n").ofType[Int].is(2))),
          ws("first").setCheck.await(1.second)(ws.checkTextMessage("first").matching(jsonPath("$.n").ofType[Int].is(1))),
          ws("close").close
        ),
        protocolCustomizer = _.wsSetCheckBuffer(10, 5.seconds)
      )

      session.isFailed shouldBe false
    }
  }

  it should "match messages that didn't match a previous check" in {
    val handler: String => List[String] = {
      case "go" => List("""{"n":1}""", """{"n":2}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          // n=1 arrives while checking for n=2
          ws("go").sendText("go").await(1.second)(ws.checkTextMessage("second").matching(jsonPath("$.n").ofType[Int].is(2))),
          ws("first").setCheck.await(1.second)(ws.checkTextMessage("first").matching(jsonPath("$.n").ofType[Int].is(1))),
          ws("close").close
        ),
        protocolCustomizer = _.wsSetCheckBuffer(10, 5.seconds)
      )

      session.isFailed shouldBe false
    }
  }

  it should "retry the messages that didn't match a check against the next checks of the sequence" in {
    val handler: String => List[String] = {
      case "go" => List("""{"n":1}""", """{"n":2}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          ws("go").sendText("go"),
          pause(500.millis),
          // n=1 doesn't match the first check but matches the second one
          ws("both").setCheck.await(1.second)(
            ws.checkTextMessage("second").matching(jsonPath("$.n").ofType[Int].is(2)),
            ws.checkTextMessage("first").matching(jsonPath("$.n").ofType[Int].is(1))
          ),
          ws("close").close
        ),
        protocolCustomizer = _.wsSetCheckBuffer(10, 5.seconds)
      )

      session.isFailed shouldBe false
    }
  }

  it should "not match messages received before the check was set by default" in {
    val handler: String => List[String] = {
      case "go" => List("""{"n":1}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          ws("go").sendText("go"),
          pause(500.millis),
          ws("first").setCheck.await(1.second)(ws.checkTextMessage("first").check(jsonPath("$.n").ofType[Int].is(1))),
          ws("close").close
        )
      )

      session.isFailed shouldBe true
    }
  }

  it should "consume messages received before the check was set" in {
    val handler: String => List[String] = {
      case "go" => List("""{"n":1}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          ws("go").sendText("go"),
          pause(500.millis),
          ws("first").setCheck.await(1.second)(ws.checkTextMessage("first").check(jsonPath("$.n").ofType[Int].is(1))),
          ws("again").setCheck.await(1.second)(ws.checkTextMessage("again").check(jsonPath("$.n").ofType[Int].is(1))),
          ws("close").close
        ),
        protocolCustomizer = _.wsSetCheckBuffer(10, 5.seconds)
      )

      session.isFailed shouldBe true
    }
  }

  it should "not match messages older than the buffer max age" in {
    val handler: String => List[String] = {
      case "go" => List("""{"n":1}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          ws("go").sendText("go"),
          pause(500.millis),
          ws("first").setCheck.await(1.second)(ws.checkTextMessage("first").check(jsonPath("$.n").ofType[Int].is(1))),
          ws("close").close
        ),
        protocolCustomizer = _.wsSetCheckBuffer(10, 100.millis)
      )

      session.isFailed shouldBe true
    }
  }

  "processUnmatchedMessages" should "receive the messages that didn't match the pending check" in {
    val handler: String => List[String] = {
      case "go" => List("""{"n":1}""", """{"n":2}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { _ =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url),
          // n=1 arrives while checking for n=2
          ws("go").sendText("go").await(1.second)(ws.checkTextMessage("second").matching(jsonPath("$.n").ofType[Int].is(2))),
          ws.processUnmatchedMessages((messages, session) => session.set("unmatched", messages.collect { case WsInboundMessage.Text(_, text) => text })),
          ws("close").close
        ),
        protocolCustomizer = _.wsUnmatchedInboundMessageBufferSize(10)
      )

      session.isFailed shouldBe false
      session("unmatched").as[List[String]] shouldBe List("""{"n":1}""")
    }
  }

  "a connection's autoReplyTextFrame" should "take precedence over the protocol's, which still applies to the other frames" in {
    val handler: String => List[String] = {
      case "go" => List("heartbeat", "tick", """{"n":1}""")
      case _    => Nil
    }

    runWithWebSocketServer(handler) { server =>
      val session = runScenario(
        scenario("ws").exec(
          ws("connect").connect(url).autoReplyTextFrame { case "heartbeat" => "connection" },
          // auto replied frames are consumed, so they don't fail the check
          ws("go").sendText("go").await(10.seconds)(ws.checkTextMessage("n").check(jsonPath("$.n").ofType[Int].is(1))),
          ws("close").close
        ),
        protocolCustomizer = _.wsAutoReplyTextFrame {
          case "heartbeat" => "protocol"
          case "tick"      => "tock"
        }
      )

      session.isFailed shouldBe false
      server.messages should contain allOf ("connection", "tock")
      server.messages should not contain "protocol"
    }
  }
}
