/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

//#dsl-bad
for (let i = 0; i < 5; i++) {
  http("Access Github").get("https://github.com");
}
//#dsl-bad

//#dsl-immutable
const request1 = http("Access Github").get("https://github.com");
// request1 is left unchanged
const request2 = request1.header("accept-encoding", "gzip");
//#dsl-immutable

//#simple-scenario
scenario("Standard User")
  .exec(
    http("Access Github").get("https://github.com"),
    pause(2, 3),
    http("Search for 'gatling'").get("https://github.com/search?q=gatling"),
    pause(2)
  );
//#simple-scenario

//#example-definition
const stdUser = scenario("Standard User"); // etc..
const admUser = scenario("Admin User"); // etc..
const advUser = scenario("Advanced User"); // etc..

setUp(
  stdUser.injectOpen(atOnceUsers(2000)),
  admUser.injectOpen(nothingFor(60), rampUsers(5).during(400)),
  advUser.injectOpen(rampUsers(500).during(200))
);
//#example-definition
