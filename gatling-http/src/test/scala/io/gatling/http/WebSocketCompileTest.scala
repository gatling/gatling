/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class WebSocketCompileTest extends Simulation {

  val httpConf = http
    .baseURL("http://localhost:9000")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
    .wsBaseURL("ws://localhost:9000")
    .wsReconnect
    .wsMaxReconnects(3)

  val scn = scenario("WebSocket")
    .exec(http("Home").get("/"))
    .pause(1)
    .exec(session => session.set("id", "Steph" + session.userId))
    .exec(http("Login").get("/room?username=${id}"))
    .pause(1)
    .exec(websocket("Connect WS").open("/room/chat?username=${id}"))
    .pause(1)
    .repeat(1, "i") {
      exec(websocket("Say Hello WS").sendTextMessage( """{"text": "Hello, I'm ${id} and this is message ${i}!"}"""))
        .pause(1)
    }.exec(websocket("Close WS").close())

  setUp(scn.inject(rampUsers(100) over 10)).protocols(httpConf)
}
