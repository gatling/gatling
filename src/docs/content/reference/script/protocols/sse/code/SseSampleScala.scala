/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

class SseSampleScala {
//#sseName
sse("Sse").sseName("myCustomName")
//#sseName

//#sseConnect
exec(sse("Connect").get("/stocks/prices"))
exec(sse("Connect").post("/stocks/prices").body(StringBody("""{"foo": "bar"}""")))
//#sseConnect

//#sseClose
exec(sse("Close").close)
//#sseClose

//#create-single-check
val sseCheck = sse.checkMessage("checkName")
  .check(regex("event: snapshot(.*)"))
//#create-single-check

//#create-multiple-checks
sse.checkMessage("checkName")
  .check(
    regex("event: event1(.*)"),
    regex("event: event2(.*)")
  )
//#create-multiple-checks

//#check-from-connect
exec(sse("Connect").get("/stocks/prices")
  .await(5)(sseCheck))
//#check-from-connect

//#check-from-flow
exec(sse("SetCheck").setCheck
  .await(30)(sseCheck))
//#check-from-flow

private val sseCheck1 = sseCheck
private val sseCheck2 = sseCheck

//#check-single-sequence
// expecting 2 messages
// 1st message will be validated against sseCheck1
// 2nd message will be validated against sseCheck2
// whole sequence must complete withing 30 seconds
exec(sse("SetCheck").setCheck
  .await(30)(sseCheck1, sseCheck2))
//#check-single-sequence

//#check-multiple-sequence
// expecting 2 messages
// 1st message will be validated against sseCheck1
// 2nd message will be validated against sseCheck2
// both sequences must complete withing 15 seconds
// 2nd sequence will start after 1st one completes
exec(sse("SetCheck").setCheck
  .await(15)(sseCheck1)
  .await(15)(sseCheck2))
//#check-multiple-sequence

//#check-matching
exec(sse("SetCheck").setCheck
  .await(1)(
    sse.checkMessage("checkName")
      .matching(substring("event"))
      .check(regex("event: snapshot(.*)"))
  ))
//#check-matching

//#process
exec(
  // store the unmatched messages in the Session
  sse.processUnmatchedMessages((messages, session) => session.set("messages", messages))
)
exec(
  // collect the last message and store it in the Session
  sse.processUnmatchedMessages { (messages, session) =>
    messages
      .lastOption
      .fold(session)(m => session.set("lastMessage", m.message))
  }
)
//#process

//#protocol
http
  // enable unmatched SSE inbound messages buffering,
  // with a max buffer size of 5
  .sseUnmatchedInboundMessageBufferSize(5)
//#protocol

//#stock-market-sample
val scn = scenario("ServerSentEvents")
  .exec(
    sse("Stocks").get("/stocks/prices")
      .await(10)(
        sse.checkMessage("checkName").check(regex("event: snapshot(.*)"))
      ),
    pause(15),
    sse("Close").close
  )
//#stock-market-sample
}
