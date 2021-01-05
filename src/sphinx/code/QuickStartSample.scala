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

//#quickstart-recorder-output
package computerdatabase // 1

import scala.concurrent.duration._

import io.gatling.core.Predef._ // 2
import io.gatling.http.Predef._

class BasicSimulation extends Simulation { // 3

  val httpProtocol = http // 4
    .baseUrl("http://computer-database.gatling.io") // 5
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // 6
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val scn = scenario("BasicSimulation") // 7
    .exec(
      http("request_1") // 8
        .get("/")
    ) // 9
    .pause(5) // 10

  setUp( // 11
    scn.inject(atOnceUsers(1)) // 12
  ).protocols(httpProtocol) // 13
}
//#quickstart-recorder-output
