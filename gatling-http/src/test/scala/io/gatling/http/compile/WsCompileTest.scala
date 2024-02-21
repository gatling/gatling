/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.http.compile

import java.nio.charset.StandardCharsets.UTF_8

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.action.ws.WsInboundMessage

class WsCompileTest extends Simulation {
  private val httpProtocol = http
    .wsBaseUrl("ws://localhost:9000")
    .wsReconnect
    .wsMaxReconnects(3)
    .wsAutoReplyTextFrame { case "ping" => "pong"; case "1" => "2" }
    .wsAutoReplySocketIo4
    .wsUnmatchedInboundMessageBufferSize(5)

  private val scn = scenario("WebSocket")
    .exec(http("Home").get("/"))
    .pause(1)
    .exec(session => session.set("id", s"Steph ${session.userId}"))
    .exec(http("Login").get("/room?username=#{id}"))
    .pause(1)
    .exec(
      ws("Connect WS")
        .connect("/room/chat?username=#{id}")
        .subprotocol("FOO")
        .await(1.second)(
          ws.checkTextMessage("checkText")
            .matching(jsonPath("$.uuid").is("#{correlation}"))
            .check(
              jsonPath("$.code").ofType[Int].is(1),
              jmesPath("code").ofType[Int].is(1),
              bodyString.is("echo")
            ),
          ws.checkBinaryMessage("checkBinary")
            .check(bodyBytes)
        )
        .await(1) { // simple int
          ws.checkTextMessage("checkName")
        }
        .await("#{someLongOrFiniteDurationAttribute}") { // EL string
          ws.checkTextMessage("checkName")
        }
        .await(_ => 1.second) { // expression
          ws.checkTextMessage("checkName")
        }
        .onConnected(
          exec(
            ws("Perform auth")
              .sendText("Some auth token")
          ).pause(1)
        ),
      ws("Say Hello WS")
        .sendText("""{"text": "Hello, I'm #{id} and this is message #{i}!"}"""),
      ws("Message1")
        .wsName("foo")
        .sendText("""{"text": "Hello, I'm #{id} and this is message #{i}!"}""")
        .await(30.seconds)(
          ws.checkTextMessage("checkName1").check(jsonPath("$.message").findAll.saveAs("message1"))
        )
        .await(30)( // simple int
          ws.checkTextMessage("checkName2").check(jsonPath("$.message").findAll.saveAs("message2"))
        )
        .await("#{someLongOrFiniteDurationAttribute}") { // EL string
          ws.checkTextMessage("checkName2").check(jsonPath("$.message").findAll.saveAs("message2"))
        }
        .await(_ => 30.seconds)( // expression
          ws.checkTextMessage("checkName2").check(jsonPath("$.message").findAll.saveAs("message2"))
        ),
      ws("Message2")
        .sendText("""{"text": "Hello, I'm #{id} and this is message #{i}!"}""")
        .await(30.seconds)(
          ws.checkTextMessage("checkName1")
            .check(
              regex("somePattern1").saveAs("message1"),
              regex("somePattern2").saveAs("message2"),
              checkIf("#{cond}") {
                regex("somePattern1")
              }
            ),
          ws.checkTextMessage("checkName2").check(regex("somePattern2").saveAs("message2"))
        ),
      ws("Message3")
        .sendText("""{"text": "Hello, I'm #{id} and this is message #{i}!"}""")
        .await(30.seconds)(
          // match first message
          ws.checkTextMessage("checkName")
        ),
      ws("BinaryMessage")
        .sendBytes("hello".getBytes(UTF_8))
        .await(30.seconds)(
          // match first message
          ws.checkBinaryMessage("checkName")
            .check(
              bodyLength.lte(50),
              bodyBytes.transform(_.length).saveAs("bytesLength"),
              checkIf("#{cond}") {
                bodyLength.lte(10)
              }
            )
            .silent
        ),
      ws("Close WS").close,
      ws("Close WS").close(1000, "Bye"),
      ws("Open Named", "foo").connect("/bar"),
      ws("SendTextMessageWithElFileBody")
        .sendText(ElFileBody("pathToSomeFile")),
      ws("SendTextMessageWithPebbleStringBody")
        .sendText(PebbleStringBody("somePebbleString")),
      ws("SendTextMessageWithPebbleFileBody")
        .sendText(PebbleFileBody("pathToSomeFile")),
      ws("SendBytesMessageWithRawFileBody")
        .sendBytes(RawFileBody("pathToSomeFile")),
      ws("SendBytesMessageWithByteArrayBody")
        .sendBytes(ByteArrayBody("#{someByteArray}")),
      ws.processUnmatchedMessages((messages, session) => session.set("messages", messages)),
      ws.processUnmatchedMessages(
        "wsName",
        (messages, session) => {
          val lastTextMessage = messages.reverseIterator.collectFirst { case WsInboundMessage.Text(_, text) =>
            text
          }

          lastTextMessage.fold(session)(m => session.set("lastTextMessage", m))
        }
      )
    )
}
