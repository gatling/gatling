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

package io.gatling.http.compile

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SseCompileTest extends Simulation {

  private val scn = scenario(this.getClass.getSimpleName)
    .exec(
      sse("connect")
        .sseName("sse")
        .connect("/stocks/prices")
        .await(30.seconds)(
          sse.checkMessage("checkName1").check(regex("""event: snapshot(.*)"""))
        )
    )
    .exec(
      sse("waitForSomeMEssage").setCheck.await(30.seconds)(
        sse.checkMessage("checkName1").check(jsonPath("$.foo"), jmesPath("foo"))
      )
    )
    .pause(15)
    .exec(sse("close").close)
    .exec(sse("foo", "bar").connect("url"))
}
