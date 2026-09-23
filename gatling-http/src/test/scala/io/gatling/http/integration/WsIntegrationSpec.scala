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
