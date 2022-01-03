/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package computerdatabase.advanced;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

public class AdvancedSimulationStep04 extends Simulation {

  FeederBuilder<String> feeder = csv("search.csv").random();

  ChainBuilder search =
      exec(http("Home").get("/"))
          .pause(1)
          .feed(feeder)
          .exec(
              http("Search")
                  .get("/computers?f=#{searchCriterion}")
                  .check(css("a:contains('#{searchComputerName}')", "href").saveAs("computerUrl")))
          .pause(1)
          .exec(http("Select").get("#{computerUrl}").check(status().is(200)))
          .pause(1);

  // repeat is a loop resolved at RUNTIME
  ChainBuilder browse =
      // Note how we force the counter name so we can reuse it
      repeat(4, "i").on(exec(http("Page #{i}").get("/computers?p=#{i}")).pause(1));

  ChainBuilder edit =
      exec(http("Form").get("/computers/new"))
          .pause(1)
          .exec(
              http("Post")
                  .post("/computers")
                  .formParam("name", "Beautiful Computer")
                  .formParam("introduced", "2012-05-30")
                  .formParam("discontinued", "")
                  .formParam("company", "37"));

  HttpProtocolBuilder httpProtocol =
      http.baseUrl("http://computer-database.gatling.io")
          .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          .doNotTrackHeader("1")
          .acceptLanguageHeader("en-US,en;q=0.5")
          .acceptEncodingHeader("gzip, deflate")
          .userAgentHeader(
              "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0");

  ScenarioBuilder users = scenario("Users").exec(search, browse);
  ScenarioBuilder admins = scenario("Admins").exec(search, browse, edit);

  {
    setUp(users.injectOpen(rampUsers(10).during(10)), admins.injectOpen(rampUsers(2).during(10)))
        .protocols(httpProtocol);
  }
}
