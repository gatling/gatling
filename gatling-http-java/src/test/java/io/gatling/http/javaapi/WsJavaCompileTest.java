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

package io.gatling.http.javaapi;

import static io.gatling.core.javaapi.Predef.*;
import static io.gatling.http.javaapi.Predef.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.gatling.core.javaapi.*;
import java.time.Duration;
import java.util.Collections;

public class WsJavaCompileTest extends Simulation {

  private HttpProtocolBuilder httpProtocol =
      http()
          .wsBaseUrl("ws://localhost:9000")
          .wsBaseUrls("url1", "url2")
          .wsBaseUrls(Collections.singletonList("url"))
          .wsReconnect()
          .wsMaxReconnects(1)
          .wsAutoReplyTextFrame(txt -> txt.equals("foo") ? "bar" : null)
          .wsAutoReplySocketIo4();

  private ChainBuilder chain =
      exec(ws("Connect WS")
              .connect("/room/chat?username=${id}")
              .subprotocol("FOO")
              .onConnected(exec(ws("Perform auth").sendText("Some auth token")).pause(1))
              .await(1)
              .on(
                  ws.checkTextMessage("checkName")
                      .matching(jsonPath("$.uuid").is("${correlation}"))
                      .check(
                          jsonPath("$.code").ofInt().is(1),
                          jmesPath("code").ofInt().is(1),
                          bodyString().is("echo")))
              .await(1)
              .on( // simple int
                  ws.checkTextMessage("checkName"))
              .await("${someLongOrFiniteDurationAttribute}")
              .on( // EL string
                  ws.checkTextMessage("checkName"))
              .await(session -> Duration.ofSeconds(1))
              .on( // function
                  ws.checkTextMessage("checkName")))
          .pause(1)
          .repeat(2, "i")
          .loop(
              exec(ws("Say Hello WS")
                      .sendText("{\"text\": \"Hello, I'm ${id} and this is message ${i}!\"}"))
                  .pause(1))
          .exec(
              ws("Message1")
                  .wsName("foo")
                  .sendText("{\"text\": \"Hello, I'm ${id} and this is message ${i}!\"}")
                  .await(Duration.ofSeconds(30))
                  .on(
                      ws.checkTextMessage("checkName1")
                          .check(jsonPath("$.message").findAll().saveAs("message1")))
                  .await(30)
                  .on( // simple int
                      ws.checkTextMessage("checkName2")
                          .check(jsonPath("$.message").findAll().saveAs("message2")))
                  .await("${someLongOrFiniteDurationAttribute}")
                  .on( // EL string
                      ws.checkTextMessage("checkName2")
                          .check(jsonPath("$.message").findAll().saveAs("message2")))
                  .await(session -> Duration.ofSeconds(30))
                  .on( // expression
                      ws.checkTextMessage("checkName2")
                          .check(jsonPath("$.message").findAll().saveAs("message2"))))
          .exec(
              ws("Message2")
                  .sendText("{\"text\": \"Hello, I'm ${id} and this is message ${i}!\"}")
                  .await(30)
                  .on(
                      ws.checkTextMessage("checkName1")
                          .check(
                              regex("somePattern1").saveAs("message1"),
                              regex("somePattern2").saveAs("message2")),
                      ws.checkTextMessage("checkName2")
                          .check(regex("somePattern2").saveAs("message2"))))
          .exec(
              ws("Message3")
                  .sendText("{\"text\": \"Hello, I'm ${id} and this is message ${i}!\"}")
                  .await(30)
                  .on(
                      // match first message
                      ws.checkTextMessage("checkName")))
          .exec(
              ws("BinaryMessage")
                  .sendBytes("hello".getBytes(UTF_8))
                  .await(30)
                  .on(
                      // match first message
                      ws.checkBinaryMessage("checkName")
                          .check(
                              bodyLength().lte(50),
                              bodyBytes().transform(bytes -> bytes.length).saveAs("bytesLength"))
                          .silent()))
          .exec(ws("Close WS").close())
          .exec(ws("Open Named", "foo").connect("/bar"))
          .exec(ws("SendTextMessageWithElFileBody").sendText(ElFileBody("pathToSomeFile")))
          .exec(
              ws("SendTextMessageWithPebbleStringBody")
                  .sendText(PebbleStringBody("somePebbleString")))
          .exec(ws("SendTextMessageWithPebbleFileBody").sendText(PebbleFileBody("pathToSomeFile")))
          .exec(ws("SendBytesMessageWithRawFileBody").sendBytes(RawFileBody("pathToSomeFile")))
          .exec(
              ws("SendBytesMessageWithByteArrayBody").sendBytes(ByteArrayBody("${someByteArray}")));
}
