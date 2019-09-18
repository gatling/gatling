/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package computerdatabase.advanced

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.concurrent.ThreadLocalRandom

class AdvancedSimulationStep05 extends Simulation {

  object Search {

    val feeder = csv("search.csv").random

    val search = exec(
      http("Home")
        .get("/")
    ).pause(1)
      .feed(feeder)
      .exec(
        http("Search")
          .get("/computers?f=${searchCriterion}")
          .check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL"))
      )
      .pause(1)
      .exec(
        http("Select")
          .get("${computerURL}")
          .check(status.is(200))
      )
      .pause(1)
  }

  object Browse {

    // repeat is a loop resolved at RUNTIME
    val browse = repeat(4, "i") { // Note how we force the counter name so we can reuse it
      exec(
        http("Page ${i}")
          .get("/computers?p=${i}")
      ).pause(1)
    }
  }

  object Edit {

    // Note we should be using a feeder here
    // let's demonstrate how we can retry: let's make the request fail randomly and retry a given number of times

    val edit = tryMax(2) { // let's try at max 2 times
      exec(
        http("Form")
          .get("/computers/new")
      ).pause(1)
        .exec(
          http("Post")
            .post("/computers")
            .formParam("name", "Beautiful Computer")
            .formParam("introduced", "2012-05-30")
            .formParam("discontinued", "")
            .formParam("company", "37")
            .check(status.is(session => 200 + ThreadLocalRandom.current.nextInt(2)))
        ) // we do a check on a condition that's been customized with a lambda. It will be evaluated every time a user executes the request
    }.exitHereIfFailed // if the chain didn't finally succeed, have the user exit the whole scenario
  }

  val httpProtocol = http
    .baseUrl("http://computer-database.gatling.io")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val users = scenario("Users").exec(Search.search, Browse.browse)
  val admins = scenario("Admins").exec(Search.search, Browse.browse, Edit.edit)

  setUp(
    users.inject(rampUsers(10) during (10 seconds)),
    admins.inject(rampUsers(2) during (10 seconds))
  ).protocols(httpProtocol)
}
