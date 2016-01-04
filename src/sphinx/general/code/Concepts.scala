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
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class Concepts extends Simulation {

  //#simple-scenario
  scenario("Standard User")
    .exec(http("Access Github").get("https://github.com"))
    .pause(2, 3)
    .exec(http("Search for 'gatling'").get("https://github.com/search?q=gatling"))
    .pause(2)
  //#simple-scenario

  //#example-definition
  val stdUser = scenario("Standard User") // etc..
  val admUser = scenario("Admin User") // etc..
  val advUser = scenario("Advanced User") // etc..

  setUp(
    stdUser.inject(atOnceUsers(2000)),
    admUser.inject(nothingFor(60 seconds), rampUsers(5) over (400 seconds)),
    advUser.inject(rampUsers(500) over (200 seconds))
  )
  //#example-definition
}
