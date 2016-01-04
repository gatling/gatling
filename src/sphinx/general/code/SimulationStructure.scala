/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

class SimulationStructure extends Simulation {

  val httpConf = http

  //#headers
  val headers_10 = Map("Content-Type" -> """application/x-www-form-urlencoded""")
  //#headers

  //#scenario-definition
  val scn = scenario("ScenarioName") // etc...
  //#scenario-definition

  //#http-request-sample
  // Here's an example of a POST request
  http("request_10")
    .post("/computers")
    .headers(headers_10)
    .formParam("name", "Beautiful Computer")
    .formParam("introduced", "2012-05-30")
    .formParam("discontinued", "")
    .formParam("company", "37")
  //#http-request-sample

  //#setUp
  setUp(
    scn.inject(atOnceUsers(1)) // (1)
      .protocols(httpConf) // (2)
  )
  //#setUp

  //#hooks
  before {
    println("Simulation is about to start!")
  }

  after {
    println("Simulation is finished!")
  }
  //#hooks
}
