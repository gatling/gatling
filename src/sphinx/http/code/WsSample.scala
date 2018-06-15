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

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class WsSample {

  //#wsName
  ws("WS Operation").wsName("myCustomName")
  //#wsName

  //#wsOpen
  exec(ws("Connect WS").connect("/room/chat?username=steph"))
  //#wsOpen

  //#wsClose
  exec(ws("Close WS").close)
  //#wsClose

  //#sendText
  exec(ws("Message")
    .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}"""))
  //#sendText

  val myCheck = ws.checkTextMessage("checkName").check(regex("hello (.*)").saveAs("name"))

  //#check-from-message
  exec(ws("Send").sendText("hello").await(30 seconds)(myCheck))
  //#check-from-message

  //#check-from-flow
  // FIXME setCheck is missing for WebSocket support
  //  exec(ws("Set Check").setCheck.check(myCheck))
  //#check-from-flow

  //#cancel-check
  // FIXME remove cancelCheck
  //  exec(ws("Cancel Check").cancelCheck)
  //#cancel-check

  //#check-example
  exec(
    ws("Send Message")
      .sendText("hello, I'm Stephane")
      .await(30 seconds)(
        ws.checkTextMessage("checkName").check(regex("hello (.*)").saveAs("name"))
      )
  )
  //#check-example

  //#reconcile
  // FIXME remove reconcile for now
  //  exec(ws("Reconcile states").reconcile)
  //#reconcile

  //#chatroom-example
  val httpConf = http
    .baseURL("http://localhost:9000")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling2")
    .wsBaseURL("ws://localhost:9000")

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
        .await(30 seconds)(
          ws.checkTextMessage("checkName").check(regex(".*I'm still alive.*"))
        )).pause(1)
    }
    .exec(ws("Close WS").close)
  //#chatroom-example
}
