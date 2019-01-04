/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class WsCompileTest extends Simulation {

  private val httpProtocol = http
    .wsBaseUrl("ws://localhost:9000")
    .wsReconnect
    .wsMaxReconnects(3)

  private val scn = scenario("WebSocket")
    .exec(http("Home").get("/"))
    .pause(1)
    .exec(session => session.set("id", "Steph" + session.userId))
    .exec(http("Login").get("/room?username=${id}"))
    .pause(1)
    .exec(ws("Connect WS").connect("/room/chat?username=${id}")
      .subprotocol("FOO")
      .await(1 second) {
        ws.checkTextMessage("checkName")
          .matching(jsonPath("$.uuid").is("${correlation}"))
          .check(jsonPath("$.code").ofType[Int].is(1))
      }
      .await(1) { // simple int
        ws.checkTextMessage("checkName")
      }
      .onConnected(
        exec(ws("Perform auth")
          .sendText("Some auth token"))
          .pause(1)
      ))
    .pause(1)
    .repeat(2, "i") {
      exec(ws("Say Hello WS")
        .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}"""))
        .pause(1)
    }
    .exec(ws("Message1").wsName("foo")
      .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}""")
      .await(30 seconds)(
        ws.checkTextMessage("checkName1").check(jsonPath("$.message").findAll.saveAs("message1"))
      )
      .await(30 seconds)(
        ws.checkTextMessage("checkName2").check(jsonPath("$.message").findAll.saveAs("message2"))
      ))
    .exec(ws("Message2")
      .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}""")
      .await(30 seconds)(
        ws.checkTextMessage("checkName1").check(
          regex("somePattern1").saveAs("message1"),
          regex("somePattern2").saveAs("message2")
        ),
        ws.checkTextMessage("checkName2").check(regex("somePattern2").saveAs("message2"))
      ))
    .exec(ws("Message3")
      .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}""")
      .await(30 seconds)(
        // match first message
        ws.checkTextMessage("checkName")
      ))
    .exec(ws("BinaryMessage")
      .sendBytes("hello".getBytes())
      .await(30 seconds)(
        // match first message
        ws.checkBinaryMessage("checkName").check(bodyBytes.transform(_.length).saveAs("bytesLength"))
      ))
    .exec(ws("Close WS").close)
    .exec(ws("Open Named", "foo").connect("/bar"))
}
