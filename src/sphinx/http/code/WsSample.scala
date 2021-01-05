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

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class WsSample {

  //#wsName
  ws("WS Operation").wsName("myCustomName")
  //#wsName

  //#connect
  exec(ws("Connect WS").connect("/room/chat?username=steph"))
  //#wsConnect

  //#subprotocol
  exec(ws("Connect WS").connect("/room/chat?username=steph").subprotocol("custom"))
  //#subprotocol

  //#onConnected
  exec(
    ws("Connect WS")
      .connect("/room/chat?username=steph")
      .onConnected(
        exec(
          ws("Perform auth")
            .sendText("Some auth token")
        ).pause(1)
      )
  )
  //#onConnected

  //#close
  exec(ws("Close WS").close)
  //#close

  //#sendText
  exec(
    ws("Message")
      .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}""")
  )
  //#sendText

  //#create-single-check
  val myCheck = ws
    .checkTextMessage("checkName")
    .check(regex("hello (.*)").saveAs("name"))
  //#create-single-check

  //#create-multiple-checks
  ws.checkTextMessage("checkName")
    .check(
      jsonPath("$.code").ofType[Int].is(1).saveAs("code"),
      jsonPath("$.message").is("OK")
    )
  //#create-multiple-checks

  //#silent-check
  ws.checkTextMessage("checkName")
    .check(regex("hello (.*)").saveAs("name"))
    .silent
  //#silent-check

  //#matching
  ws.checkTextMessage("checkName")
    .matching(jsonPath("$.uuid").is("${correlation}"))
    .check(jsonPath("$.code").ofType[Int].is(1))
  //#matching

  //#check-from-connect
  exec(ws("Connect").connect("/foo").await(30.seconds)(myCheck))
  //#check-from-connect

  //#check-from-message
  exec(ws("Send").sendText("hello").await(30.seconds)(myCheck))
  //#check-from-message

  val myCheck1 = myCheck
  val myCheck2 = myCheck

  //#check-single-sequence
  // expecting 2 messages
  // 1st message will be validated against myCheck1
  // 2nd message will be validated against myCheck2
  // whole sequence must complete withing 30 seconds
  exec(ws("Send").sendText("hello")
    .await(30.seconds)(myCheck1, myCheck2))
  //#check-single-sequence

  //#check-multiple-sequence
  // expecting 2 messages
  // 1st message will be validated against myCheck1
  // 2nd message will be validated against myCheck2
  // both sequences must complete withing 15 seconds
  // 2nd sequence will start after 1st one completes
  exec(ws("Send").sendText("hello")
    .await(15.seconds)(myCheck1)
    .await(15.seconds)(myCheck2))
  //#check-multiple-sequence

  //#check-matching
  exec(ws("Send").sendText("hello")
    .await(1.second)(
      ws.checkTextMessage("checkName")
        .matching(jsonPath("$.uuid").is("${correlation}"))
        .check(jsonPath("$.code").ofType[Int].is(1))
    ))
  //#check-matching

  //#chatroom-example
  val httpProtocol = http
    .baseUrl("http://localhost:9000")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling2")
    .wsBaseUrl("ws://localhost:9000")

  val scn = scenario("WebSocket")
    .exec(http("Home").get("/"))
    .pause(1)
    .exec(session => session.set("id", "Steph" + session.userId))
    .exec(http("Login").get("/room?username=${id}"))
    .pause(1)
    .exec(ws("Connect WS").connect("/room/chat?username=${id}"))
    .pause(1)
    .repeat(2, "i") {
      exec(ws("Say Hello WS")
        .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}""")
        .await(30.seconds)(
          ws.checkTextMessage("checkName").check(regex(".*I'm still alive.*"))
        )).pause(1)
    }
    .exec(ws("Close WS").close)
  //#chatroom-example
}
