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

class Assertions extends Simulation {

  val scn = scenario("foo").inject(atOnceUsers(1))

  //#setUp
  setUp(scn).assertions(
    global.responseTime.max.lessThan(50),
    global.successfulRequests.percent.greaterThan(95)
  )
  //#setUp

  //#details
  details("Search" / "Index")
  //#details

  //#examples
  // Assert that the max response time of all requests is less than 100 ms
  setUp(scn).assertions(global.responseTime.max.lessThan(100))

  // Assert that every request has no more than 5% of failing requests
  setUp(scn).assertions(forAll.failedRequests.percent.lessThan(5))

  // Assert that the percentage of failed requests named "Index" in the group "Search"
  // is exactly 0 %
  setUp(scn).assertions(details("Search" / "Index").failedRequests.percent.is(0))

  // Assert that the rate of requests per seconds for the group "Search"
  setUp(scn).assertions(details("Search").requestsPerSec.between(100, 1000))
  //#examples
}
