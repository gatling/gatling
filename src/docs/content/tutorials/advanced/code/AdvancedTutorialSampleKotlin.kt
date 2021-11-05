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

import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*

class AdvancedTutorialSampleKotlin {

  object Step1 : Simulation() {

//#isolate-processes
object Search {

  val search =
    // let's give proper names, as they are displayed in the reports
    exec(http("Home")
      .get("/"))
      .pause(7)
      .exec(http("Search")
      .get("/computers?f=macbook"))
      .pause(2)
      .exec(http("Select")
      .get("/computers/6"))
      .pause(3)
}

object Browse {

  val browse: ChainBuilder = TODO()
}

object Edit {

  val edit: ChainBuilder = TODO()
}
//#isolate-processes

//#processes
val scn = scenario("Scenario Name")
  .exec(Search.search, Browse.browse, Edit.edit)
//#processes

//#populations
val users = scenario("Users")
  .exec(Search.search, Browse.browse)
val admins = scenario("Admins")
  .exec(Search.search, Browse.browse, Edit.edit)
//#populations

    val httpProtocol = http
//#setup-users
init {
  setUp(users.injectOpen(atOnceUsers(10)).protocols(httpProtocol))
}
//#setup-users

//#setup-users-and-admins
init {
  setUp(
    users.injectOpen(rampUsers(10).during(10)),
    admins.injectOpen(rampUsers(2).during(10))
  ).protocols(httpProtocol)
}
//#setup-users-and-admins
  }
}

//#feeder
object Search {
  val feeder = csv("search.csv").random() // 1, 2

  val search = exec(http("Home")
    .get("/"))
    .pause(1)
    .feed(feeder) // 3
    .exec(http("Search")
      .get("/computers?f=#{searchCriterion}") // 4
      .check(
        css("a:contains('#{searchComputerName}')", "href") // 5
          .saveAs("computerURL")
      )
    )
    .pause(1)
    .exec(http("Select")
      .get("#{computerURL}")) // 6
    .pause(1)
}
//#feeder

object BrowseLoopSimple {
//#loop-simple
object Browse {
  fun gotoPage(page: Int) =
    exec(http("Page $page")
      .get("/computers?p=$page"))
      .pause(1)

  val browse =
    exec(
      gotoPage(0),
      gotoPage(1),
      gotoPage(2),
      gotoPage(3),
      gotoPage(4)
    )
}
//#loop-simple
}

object BrowseLoopFor {
//#loop-for
object Browse {
  val browse =
    repeat(5, "n").on( // 1
      exec(http("Page #{n}").get("/computers?p=#{n}")) // 2
        .pause(1)
    )
}
//#loop-for
}

object CheckAndTryMax {
//#check
val edit =
  exec(http("Form").get("/computers/new"))
    .pause(1)
    .exec(http("Post")
      .post("/computers")
      .check(status().shouldBe { session ->
        200 + java.util.concurrent.ThreadLocalRandom.current().nextInt(2)
      })
    )
//#check

//#tryMax-exitHereIfFailed
val tryMaxEdit = tryMax(2).on( // 1
  exec(edit)
).exitHereIfFailed() // 2
//#tryMax-exitHereIfFailed
}
