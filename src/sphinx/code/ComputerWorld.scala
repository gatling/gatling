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

import scala.concurrent.duration._

import java.util.concurrent.ThreadLocalRandom

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class ComputerWorld extends Simulation {

  val httpProtocol = http
    .baseURL("http://computer-database.gatling.io")
    .acceptHeader("""text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8""")
    .acceptEncodingHeader("""gzip, deflate""")
    .acceptLanguageHeader("""en-gb,en;q=0.5""")
    .contentTypeHeader("application/x-www-form-urlencoded")
    .userAgentHeader("""Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:31.0) Gecko/20100101 Firefox/31.0""")

  val computerDbScn = scenario("Computer Scenario")
    .exec(http("getComputers")
      .get("/computers")
      .check(
        status.is(200),
        regex("""\d+ computers found"""),
        css("#add", "href").saveAs("addComputer")))

    //#print-all-session-values
    .exec { session =>
      println(session)
      session
    }
    //#print-all-session-values

    //#print-session-value
    .exec { session =>
    println(session("addComputer").as[String])
    session
  }
    //#print-session-value

    .exec(http("addNewComputer")
    .get("${addComputer}")
    .check(substring("Add a computer")))

    .exec(_.set("homeComputer", s"homeComputer_${ThreadLocalRandom.current.nextInt(Int.MaxValue)}"))
    .exec(http("postComputers")
      .post("/computers")
      .formParam("name", "${homeComputer}")
      .formParam("introduced", "2015-10-10")
      .formParam("discontinued", "2017-10-10")
      .formParam("company", "")
      .check(substring("${homeComputer}")))

  setUp(computerDbScn.inject(
    constantUsersPerSec(2) during(1 minutes)
  ).protocols(httpProtocol))
}
