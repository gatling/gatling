/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class AdvancedTutorialSampleJava {

  public static final class Step1 extends Simulation {
//#isolate-processes
ChainBuilder search =
  // let's give proper names, as they are displayed in the reports
  exec(http("Home")
    .get("/"))
    .pause(7)
    .exec(http("Search")
      .get("/computers?f=macbook"))
    .pause(2)
    .exec(http("Select")
      .get("/computers/6"))
    .pause(3);

ChainBuilder browse = null; // TODO

ChainBuilder edit = null; // TODO
//#isolate-processes

//#processes
ScenarioBuilder scn = scenario("Scenario Name")
  .exec(search, browse, edit);
//#processes

//#populations
ScenarioBuilder users = scenario("Users")
  .exec(search, browse);
ScenarioBuilder admins = scenario("Admins")
  .exec(search, browse, edit);
//#populations

    HttpProtocolBuilder httpProtocol = http;

//#setup-users
{
  setUp(users.injectOpen(atOnceUsers(10)).protocols(httpProtocol));
}
//#setup-users

//#setup-users-and-admins
{
  setUp(
    users.injectOpen(rampUsers(10).during(10)),
    admins.injectOpen(rampUsers(2).during(10))
  ).protocols(httpProtocol);
}
//#setup-users-and-admins
  }

//#feeder
FeederBuilder.Batchable<String> feeder =
  csv("search.csv").random(); // 1, 2

ChainBuilder search = exec(http("Home")
  .get("/"))
  .pause(1)
  .feed(feeder) // 3
  .exec(http("Search")
    .get("/computers?f=#{searchCriterion}") // 4
    .check(
      css("a:contains('#{searchComputerName}')", "href")
        .saveAs("computerUrl") // 5
    )
  )
  .pause(1)
  .exec(http("Select")
    .get("#{computerUrl}")) // 6
  .pause(1);
//#feeder

  public static final class BrowseLoopSimple {

//#loop-simple
private static ChainBuilder gotoPage(int page) {
  return exec(http("Page " + page)
    .get("/computers?p=" + page))
    .pause(1);
}

ChainBuilder browse =
  exec(
    gotoPage(0),
    gotoPage(1),
    gotoPage(2),
    gotoPage(3),
    gotoPage(4)
  );
//#loop-simple
  }

  public static final class BrowseLoopFor {

//#loop-for
ChainBuilder browse =
  repeat(5, "n").on( // 1
    exec(http("Page #{n}").get("/computers?p=#{n}")) // 2
      .pause(1)
  );
//#loop-for
  }

  public static final class CheckAndTryMax {
//#check
ChainBuilder edit =
  exec(http("Form").get("/computers/new"))
    .pause(1)
    .exec(http("Post")
      .post("/computers")
      .formParam("name", "computer xyz")
      .check(status().is(session ->
        200 + java.util.concurrent.ThreadLocalRandom.current().nextInt(2) // 2
      ))
    );
//#check

//#tryMax-exitHereIfFailed
ChainBuilder tryMaxEdit = tryMax(2).on( // 1
  exec(edit)
).exitHereIfFailed(); // 2
//#tryMax-exitHereIfFailed
  }
}
