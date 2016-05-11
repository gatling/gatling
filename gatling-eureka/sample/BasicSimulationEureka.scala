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
import scala.concurrent.duration._

import io.gatling.eureka.Predef._
import io.gatling.eureka._

class BasicSimulationEureka extends Simulation {

  val httpConf = http
    .baseURL("http://requestb.in") 
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val headers = Map("Content-Type" -> "application/x-www-form-urlencoded")

  val eurekaIP = eureka("http://127.0.0.1:8080/eureka","EUREKA").ip

  val scn = scenario("Test Eureka Request Basic") 
    .exec(
      http("POST eureka IP")
        .post("/158u0j51")
        .headers(headers)
        .formParam("by", "Diego Pacheco") 
        .formParam("Eureka_IP", eurekaIP) 
    )

  setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
}


