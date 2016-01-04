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
package computerdatabase.advanced

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AdvancedSimulationStep03 extends Simulation {

  object Search {

    // We need dynamic data so that all users don't play the same and we end up with a behavior completely different from the live system (caching, JIT...)
    // ==> Feeders!

    val feeder = csv("search.csv").random // default is queue, so for this test, we use random to avoid feeder starvation

    val search = exec(http("Home")
      .get("/"))
      .pause(1)
      .feed(feeder) // every time a user passes here, a record is popped from the feeder and injected into the user's session
      .exec(http("Search")
        .get("/computers?f=${searchCriterion}") // use session data thanks to Gatling's EL
        .check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL"))) // use a CSS selector with an EL, save the result of the capture group
      .pause(1)
      .exec(http("Select")
        .get("${computerURL}") // use the link previously saved
        .check(status.is(200)))
      .pause(1)
  }

  object Browse {

    val browse = exec(http("Home")
      .get("/"))
      .pause(2)
      .exec(http("Page 1")
        .get("/computers?p=1"))
      .pause(670 milliseconds)
      .exec(http("Page 2")
        .get("/computers?p=2"))
      .pause(629 milliseconds)
      .exec(http("Page 3")
        .get("/computers?p=3"))
      .pause(734 milliseconds)
      .exec(http("Page 4")
        .get("/computers?p=4"))
      .pause(5)
  }

  object Edit {

    val headers_10 = Map("Content-Type" -> "application/x-www-form-urlencoded")

    val edit = exec(http("Form")
      .get("/computers/new"))
      .pause(1)
      .exec(http("Post")
        .post("/computers")
        .headers(headers_10)
        .formParam("name", "Beautiful Computer")
        .formParam("introduced", "2012-05-30")
        .formParam("discontinued", "")
        .formParam("company", "37"))
  }

  val httpConf = http
    .baseURL("http://computer-database.gatling.io")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val users = scenario("Users").exec(Search.search, Browse.browse)
  val admins = scenario("Admins").exec(Search.search, Browse.browse, Edit.edit)

  setUp(
    users.inject(rampUsers(10) over (10 seconds)),
    admins.inject(rampUsers(2) over (10 seconds))
  ).protocols(httpConf)
}
