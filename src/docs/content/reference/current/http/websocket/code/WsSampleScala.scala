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

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.check.ws.WsFrameCheck

class WsSampleScala {
//#wsName
ws("WS Operation").wsName("myCustomName")
//#wsName

//#wsConnect
exec(ws("Connect WS").connect("/room/chat?username=gatling"))
//#wsConnect

//#subprotocol
exec(ws("Connect WS").connect("/room/chat?username=gatling").subprotocol("custom"))
//#subprotocol

//#onConnected
exec(ws("Connect WS").connect("/room/chat?username=gatling")
  .onConnected(
    exec(ws("Perform auth")
      .sendText("Some auth token"))
      .pause(1)
  ))
//#onConnected

//#close
// close with a 1000 status
exec(ws("Close WS").close)
// close with arbitrary status and reason
exec(ws("Close WS").close(1007, "Invalid payload data"))
//#close

//#send
// send text with a Gatling EL string
exec(ws("Message")
  .sendText("""{"text": "Hello, I'm #{id} and this is message #{i}!"}"""))
// send text with a function
exec(ws("Message")
  .sendText(session => s"""{"text": "Hello, I'm ${session("id").as[String]} and this is message ${session("i").as[String]}!"}"""))
// send text with ElFileBody
exec(ws("Message")
  .sendText(ElFileBody("filePath")))
// send text with ElFileBody
exec(ws("Message")
  .sendText(PebbleStringBody("somePebbleTemplate")))
// send text with ElFileBody
exec(ws("Message")
  .sendText(PebbleFileBody("filePath")))

// send bytes with a Gatling EL string referencing a byte array in the Session
exec(ws("Message")
  .sendBytes("#{bytes}"))
// send bytes with a function
exec(ws("Message")
  .sendBytes(session => Array[Byte](0, 5, 3, 1)))
// send bytes with RawFileBody
exec(ws("Message")
  .sendBytes(RawFileBody("filePath")))
  // send bytes with RawFileBody
exec(ws("Message")
  .sendBytes(ByteArrayBody("#{bytes}")))
//#send

//#create-single-check
// with a static name
ws.checkTextMessage("checkName")
  .check(regex("hello (.*)").saveAs("name"))
// with a Gatling EL string name
ws.checkTextMessage("#{checkName}")
  .check(regex("hello (.*)").saveAs("name"))
// with a function name
ws.checkTextMessage(session => "checkName")
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
  .matching(jsonPath("$.uuid").is("#{correlation}"))
  .check(jsonPath("$.code").ofType[Int].is(1))
//#matching

val wsCheck: WsFrameCheck.Text = ???
val wsCheck1 = wsCheck
val wsCheck2 = wsCheck

//#check-from-connect
exec(ws("Connect").connect("/foo").await(30)(wsCheck))
//#check-from-connect

//#check-from-message
exec(ws("Send").sendText("hello").await(30)(wsCheck))
//#check-from-message

//#check-single-sequence
// expecting 2 messages
// 1st message will be validated against wsCheck1
// 2nd message will be validated against wsCheck2
// whole sequence must complete withing 30 seconds
exec(ws("Send").sendText("hello")
  .await(30)(wsCheck1, wsCheck2))
//#check-single-sequence

//#check-multiple-sequence
// expecting 2 messages
// 1st message will be validated against wsCheck1
// 2nd message will be validated against wsCheck2
// both sequences must complete withing 15 seconds
// 2nd sequence will start after 1st one completes
exec(ws("Send").sendText("hello")
  .await(15)(wsCheck1)
  .await(15)(wsCheck2)
)
//#check-multiple-sequence

//#check-matching
exec(ws("Send").sendText("hello")
  .await(1)(
    ws.checkTextMessage("checkName")
      .matching(jsonPath("$.uuid").is("#{correlation}"))
      .check(jsonPath("$.code").ofType[Int].is(1))
  ))
//#check-matching

//#protocol
http
  // similar to standard `baseUrl` for HTTP,
  // serves as root that will be prepended to all relative WebSocket urls
  .wsBaseUrl("url")
  // similar to standard `baseUrls` for HTTP,
  // serves as round-robin roots that will be prepended
  // to all relative WebSocket urls
  .wsBaseUrls("url1", "url2")
  // automatically reconnect a WebSocket that would have been
  // closed by someone else than the client.
  .wsReconnect
  // set a limit on the number of times a WebSocket will be
  // automatically reconnected
  .wsMaxReconnects(5)
  //  configure auto reply for specific WebSocket text messages.
  //  Example: `wsAutoReplyTextFrame({ case "ping" => "pong" })`
  //  will automatically reply with message `"pong"`
  //  when message `"ping"` is received.
  //  Those messages won't be visible in any reports or statistics.
  .wsAutoReplyTextFrame {
    case "ping" => "pong"
  }
  // enable partial support for Engine.IO v4.
  // Gatling will automatically respond
  // to server ping messages (`2`) with pong (`3`).
  // Cannot be used together with `wsAutoReplyTextFrame`.
  .wsAutoReplySocketIo4

  // enable to store all received messages to buffer and print to stdout
  .wsLogsPiling
//#protocol

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
  .exec(http("Login").get("/room?username=#{id}"))
  .pause(1)
  .exec(ws("Connect WS").connect("/room/chat?username=#{id}"))
  .pause(1)
  .repeat(2, "i") {
    exec(
      ws("Say Hello WS")
        .sendText("""{"text": "Hello, I'm #{id} and this is message #{i}!"}""")
        .await(30)(
          ws.checkTextMessage("checkName").check(regex(".*I'm still alive.*"))
        )
    ).pause(1)
  }
  .exec(ws("Close WS").close)
//#chatroom-example
}
