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

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.function.Function;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

class ScenarioSampleJava {

  ScenarioSampleJava() {
//#bootstrapping
scenario("My Scenario")
//#bootstrapping
;

//#exec-example
scenario("My Scenario")
  .exec(http("Get Homepage").get("http://github.com/gatling/gatling"));
//#exec-example

//#session-lambda
exec(session -> {
  // displays the content of the session in the console (debugging only)
  System.out.println(session);

  // return the original session
  return session;
});

exec(session ->
  // return a new session instance
  // with a new "foo" attribute whose value is "bar"
  session.set("foo", "bar")
);
//#session-lambda

//#pace
forever().on(
  pace(5)
    .exec(
      pause(1, 4) // Will be run every 5 seconds, irrespective of what pause time is used
    )
);
//#pace

    int times = 4;
    String sequenceName, elementName = "foo";
    ChainBuilder myChain = exec(Function.identity());
    boolean condition, exitASAP = true;
    int duration = 5;

//#repeat-example
repeat(times, "counterName").on(
  myChain
);
//#repeat-example

//#repeat-variants
// will loop on myChain 20 times
repeat(20).on(
  myChain
);

// will loop on myChain (Int value of the Session attribute myKey) times
repeat("${myKey}").on(
  myChain
);


repeat(session -> session.getInt("foo")).on(
  myChain
);
//#repeat-variants

//#foreach
foreach("${list}", elementName, "counterName").on(
  myChain
);
//#foreach

//#during
during(duration, "counterName", exitASAP).on(
  myChain
);
//#during

//#asLongAs
asLongAs("${comeCondition}", "counterName", exitASAP).on(
  myChain
);
//#asLongAs

//#doWhile
doWhile("${comeCondition}", "counterName").on(
  myChain
);
//#doWhile

//#asLongAsDuring
asLongAsDuring("${comeCondition}", duration, "counterName").on(
  myChain
);
//#asLongAsDuring

//#doWhileDuring
doWhileDuring("${comeCondition}", duration, "counterName").on(
  myChain
);
//#doWhileDuring

//#forever
forever("counterName").on(
  myChain
);
//#forever

//#doIf
doIf("${myBoolean}").then(
  // executed if the session value stored in "myBoolean" is true
  exec(http("...").get("..."))
);
//#doIf

//#doIf-session
doIf(session -> session.getString("myKey").startsWith("admin")).then(
  // executed if the session value stored in "myKey" starts with "admin"
  exec(http("if true").get("..."))
);
//#doIf-session

//#doIfEquals
doIfEquals("${actualValue}", "expectedValue").then(
  // executed if the session value stored in "actualValue" is equal to "expectedValue"
  exec(http("...").get("..."))
);
//#doIfEquals

//#doIfOrElse
doIfOrElse(session -> session.getString("myKey").startsWith("admin")).then(
  // executed if the session value stored in "myKey" starts with "admin"
  exec(http("if true").get("..."))
).orElse(
  // executed if the session value stored in "myKey" does not start with "admin"
  exec(http("if false").get("..."))
);
//#doIfOrElse

//#doIfEqualsOrElse
doIfEqualsOrElse(session -> session.getString("actualValue"), "expectedValue").then(
  // executed if the session value stored in "actualValue" equals to "expectedValue"
  exec(http("if true").get("..."))
).orElse(
  // executed if the session value stored in "actualValue" is not equal to "expectedValue"
  exec(http("if false").get("..."))
);
//#doIfEqualsOrElse

    ChainBuilder chain1 = null;
    ChainBuilder chain2 = null;
    ChainBuilder myFallbackChain = null;

//#doSwitch
doSwitch("${myKey}").on(
  Choice.withKey("key1", chain1),
  Choice.withKey("key2", chain2)
);
//#doSwitch

//#doSwitchOrElse
doSwitchOrElse("${myKey}").on(
  Choice.withKey("key1", chain1),
  Choice.withKey("key2", chain2)
).orElse(
  myFallbackChain
);
//#doSwitchOrElse

//#randomSwitch
randomSwitch().on(
  Choice.withWeight(0.6, chain1),
  Choice.withWeight(0.4, chain2)
);
//#randomSwitch

//#randomSwitchOrElse
randomSwitchOrElse().on(
  Choice.withWeight(0.6, chain1),
  Choice.withWeight(0.4, chain2)
).orElse(
  myFallbackChain
);
//#randomSwitchOrElse

//#uniformRandomSwitch
uniformRandomSwitch().on(
  chain1,
  chain2
);
//#uniformRandomSwitch

//#roundRobinSwitch
roundRobinSwitch().on(
  chain1,
  chain2
);
//#roundRobinSwitch

//#tryMax
tryMax(times, "counterName").on(
  myChain
);
//#tryMax

//#exitBlockOnFail
exitBlockOnFail(
  myChain
);
//#exitBlockOnFail

//#exitHereIf
exitHere();
//#exitHereIf

//#exitHereIf
exitHereIf("${myBoolean}");
exitHereIf(session -> true);
//#exitHereIf

//#exitHereIfFailed
exitHereIfFailed();
//#exitHereIfFailed

    String groupName = "foo";
//#group
group(groupName).on(
  myChain
);
//#group

    ScenarioBuilder scn = null;
    HttpProtocolBuilder httpProtocol = null;

//#protocol
scn.injectOpen(atOnceUsers(5)).protocols(httpProtocol);
//#protocol

//#throttling
scn.injectOpen(rampUsers(500).during(Duration.ofMinutes(10)))
  .throttle(reachRps(100).in(10), holdFor(Duration.ofMinutes(10)));
//#throttling
  }
}
