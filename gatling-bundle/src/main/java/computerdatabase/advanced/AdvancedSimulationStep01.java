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

package computerdatabase.advanced;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;

public class AdvancedSimulationStep01 extends Simulation {

  // Let's split this big scenario into composable business processes, like one would do with
  // PageObject pattern with Selenium

  private static class Search {

    static ChainBuilder search =
        exec(http("Home") // let's give proper names, they are displayed in the reports, and used as
                // keys
                .get("/"))
            .pause(1) // let's set the pauses to 1 sec for demo purpose
            .exec(http("Search").get("/computers?f=macbook"))
            .pause(1)
            .exec(http("Select").get("/computers/6"))
            .pause(1);
  }

  private static class Browse {

    static ChainBuilder browse =
        exec(http("Home").get("/"))
            .pause(2)
            .exec(http("Page 1").get("/computers?p=1"))
            .pause(Duration.ofMillis(670))
            .exec(http("Page 2").get("/computers?p=2"))
            .pause(Duration.ofMillis(629))
            .exec(http("Page 3").get("/computers?p=3"))
            .pause(Duration.ofMillis(734))
            .exec(http("Page 4").get("/computers?p=4"))
            .pause(5);
  }

  private static class Edit {

    static ChainBuilder edit =
        exec(http("Form").get("/computers/new"))
            .pause(1)
            .exec(
                http("Post")
                    .post("/computers")
                    .formParam("name", "Beautiful Computer")
                    .formParam("introduced", "2012-05-30")
                    .formParam("discontinued", "")
                    .formParam("company", "37"));
  }

  HttpProtocolBuilder httpProtocol =
      http.baseUrl("http://computer-database.gatling.io")
          .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          .doNotTrackHeader("1")
          .acceptLanguageHeader("en-US,en;q=0.5")
          .acceptEncodingHeader("gzip, deflate")
          .userAgentHeader(
              "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0");

  // Now, we can write the scenario as a composition
  ScenarioBuilder scn = scenario("Scenario Name").exec(Search.search, Browse.browse, Edit.edit);

  {
    setUp(scn.injectOpen(atOnceUsers(1)).protocols(httpProtocol));
  }
}
