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

class SseSample {

  //#sseName
  sse("SSE Operation").sseName("myCustomName")
  //#sseName

  //#sseConnect
  exec(sse("Get SSE").connect("/stocks/prices"))
  //#sseConnect

  //#sseClose
  exec(sse("Close SSE").close())
  //#sseClose

  val myCheck = sse.checkMessage("checkName").check(regex("""event: snapshot(.*)"""))

  //#check-from-message
  exec(sse("Get SSE").connect("/stocks/prices").await(5 seconds)(myCheck))
  //#check-from-message

  //#check-from-flow
  exec(sse("Set Check").setCheck.await(30 seconds)(
    myCheck
  ))
  //#check-from-flow

  //#build-check
  exec(sse("sse").connect("/stocks/prices")
    .await(30 seconds)(
      sse.checkMessage("checkName").check(regex("""event: snapshot(.*)"""))
    ))
  //#build-check

  //#stock-market-sample
  val httpConf = http
    .baseUrl("http://localhost:8080/app")

  val scn = scenario("Server Sent Event")
    .exec(
      sse("Stocks").connect("/stocks/prices")
        .await(10)(
          sse.checkMessage("checkName").check(regex("""event: snapshot(.*)"""))
        )
    )
    .pause(15)
    .exec(sse("Close SSE").close())
  //#stock-market-sample
}
