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

import io.gatling.javaapi.core.*;

import java.time.Duration;
import java.util.List;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

class ScenarioSampleJava {
  {
//#bootstrapping
ScenarioBuilder scn = scenario("Scenario");
//#bootstrapping

//#exec
// attached to a scenario
scenario("Scenario")
  .exec(http("Home").get("https://gatling.io"));

// directly created and stored in a reference
ChainBuilder chain = exec(http("Home").get("https://gatling.io"));

// executed sequentially
exec(
  http("Home").get("https://gatling.io"),
  http("Enterprise").get("https://gatling.io/enterprise")
);

// attached to another
exec(http("Home").get("https://gatling.io"))
  .exec(http("Enterprise").get("https://gatling.io/enterprise"));
//#exec

//#session-lambda
exec(session -> {
  // displays the content of the session in the console
  // WARNING: DEBUGGING ONLY, NOT UNDER LOAD
  // sysout is a slow blocking output,
  // massively writing in here will freeze Gatling's engine
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

//#session-lambda-bad
exec(session -> {
  // just creates a dangling component, doesn't produce any effect
  http("Gatling").get("https://gatling.io");
  return session;
});
//#session-lambda-bad

//#pause-fixed
// with a number of seconds
pause(10);
// with a java.time.Duration
pause(Duration.ofMillis(100));
// with a Gatling EL string resolving to a number of seconds or a java.time.Duration
pause("#{pause}");
// with a function that returns a java.time.Duration
pause(session -> Duration.ofMillis(100));
//#pause-fixed

//#pause-uniform
// with a number of seconds
pause(10, 20);
// with a java.time.Duration
pause(Duration.ofMillis(100), Duration.ofMillis(200));
// with a Gatling EL strings
pause("#{min}", "#{max}");
// with a function that returns a java.time.Duration
pause(session -> Duration.ofMillis(100), session -> Duration.ofMillis(200));
//#pause-uniform

//#pace
forever().on(
  pace(5)
    .exec(
      // will be run every 5 seconds, irrespective of what pause time is used
      pause(1, 4)
    )
);
//#pace

//#pace-fixed
// with a number of seconds
pace(10);
// with a java.time.Duration
pace(Duration.ofMillis(100));
// with a Gatling EL string resolving to a number of seconds or a java.time.Duration
pace("#{pace}");
// with a function that returns a java.time.Duration
pace(session -> Duration.ofMillis(100));
//#pace-fixed

//#pace-uniform
// with a number of seconds
pace(10, 20);
// with a java.time.Duration
pace(Duration.ofMillis(100), Duration.ofMillis(200));
// with a Gatling EL strings
pace("#{min}", "#{max}");
// with a function that returns a java.time.Duration
pace(session -> Duration.ofMillis(100), session -> Duration.ofMillis(200));
//#pace-uniform

//#rendezVous
rendezVous(100);
//#rendezVous

//#repeat
// with an Int times
repeat(5).on(
  http("name").get("/")
);
// with a Gatling EL string resolving an Int
repeat("#{times}").on(
  http("name").get("/")
);
// with a function times
repeat(session -> 5).on(
  http("name").get("/")
);
// with a counter name
repeat(5, "counter").on(
  http("name").get("/?counter=#{counter}")
);
// iterating over multiple actions sequentially
repeat(5).on(
  http("name1").get("/"),
  http("name2").get("/")
);
//#repeat

//#foreach
// with a static List
foreach(List.of("elt1", "elt2"), "elt").on(
  http("name").get("/?elt=#{elt}")
);
// with a Gatling EL string
foreach("#{elts}", "elt").on(
  http("name").get("/?elt=#{elt}")
);
// with a function
foreach(session -> List.of("elt1", "elt2"), "elt").on(
  http("name").get("/?elt=#{elt}")
);
// with a counter name
foreach(List.of("elt1", "elt2"), "elt", "counter").on(
  http("name").get("/?elt=#{elt}&counter=#{counter}")
);
// iterating over multiple actions sequentially
foreach(List.of("elt1", "elt2"), "elt").on(
  http("name1").get("/?elt=#{elt}"),
  http("name2").get("/?elt=#{elt}")
);
//#foreach

//#during
// with a duration in seconds
during(5).on(
  http("name").get("/")
);
// with a java.time.Duration
during(Duration.ofMinutes(10)).on(
  http("name").get("/")
);
// with a Gatling EL string resolving a duration
during("#{times}").on(
  http("name").get("/")
);
// with a function times
during(session -> Duration.ofMinutes(10)).on(
  http("name").get("/")
);
// with a counter name
during(5, "counter").on(
  http("name").get("/?counter=#{counter}")
);
// with exitASAP
during(5, "counter", false).on(
  http("name").get("/?counter=#{counter}")
);
// iterating over multiple actions sequentially
during(5).on(
  http("name1").get("/"),
  http("name2").get("/")
);
//#during

//#asLongAs
// with a Gatling EL string resolving to a boolean
asLongAs("#{condition}").on(
  http("name").get("/")
);
// with a function
asLongAs(session -> session.getBoolean("condition")).on(
  http("name").get("/")
);
// with a counter name and exitASAP
asLongAs("#{condition}", "counter", false).on(
  http("name").get("/?counter=#{counter}")
);
// iterating over multiple actions sequentially
asLongAs("#{condition}").on(
  http("name1").get("/"),
  http("name2").get("/")
);
//#asLongAs

//#doWhile
// with a Gatling EL string resolving to a boolean
doWhile("#{condition}").on(
  http("name").get("/")
);
// with a function
doWhile(session -> session.getBoolean("condition")).on(
  http("name").get("/")
);
// with a counter name
doWhile("#{condition}", "counter").on(
  http("name").get("/?counter=#{counter}")
);
// iterating over multiple actions
doWhile("#{condition}").on(
  http("name1").get("/"),
  http("name2").get("/")
);
//#doWhile

//#asLongAsDuring
// with a Gatling EL string resolving to a boolean and an int duration
asLongAsDuring("#{condition}", 5).on(
  http("name").get("/")
);
// with a counter name and exitASAP
asLongAsDuring(session -> true, Duration.ofMinutes(10), "counter", false).on(
  http("name").get("/?counter=#{counter}")
);
// iterating over multiple actions
asLongAsDuring("#{condition}", 5).on(
  http("name1").get("/"),
  http("name2").get("/")
);
//#asLongAsDuring

//#doWhileDuring
// with a Gatling EL string resolving to a boolean and an int duration
doWhileDuring("#{condition}", 5).on(
  http("name").get("/")
);
// with a counter name and exitASAP
doWhileDuring(session -> true, Duration.ofMinutes(10), "counter", false).on(
  http("name").get("/?counter=#{counter}")
);
// iterating over multiple actions sequentially
doWhileDuring("#{condition}", 5).on(
  http("name1").get("/"),
  http("name2").get("/")
);
//#doWhileDuring

//#forever
forever().on(
  http("name").get("/")
);
// with a counter name
forever("counter").on(
  http("name").get("/?counter=#{counter}")
);
// iterating over multiple actions sequentially
forever().on(
  http("name1").get("/"),
  http("name2").get("/")
);
//#forever

//#doIf
// with a Gatling EL string resolving to a boolean
doIf("#{condition}").then(
  http("name").get("/")
);
// with a function
doIf(session -> session.getBoolean("condition")).then(
  http("name").get("/")
);
// executing multiple actions
doIf("#{condition}").then(
  http("name1").get("/"),
  http("name2").get("/")
);
//#doIf

//#doIfEquals
doIfEquals("#{actual}", "expectedValue").then(
  // executed if the session value stored in "actual" is equal to "expectedValue"
  http("name").get("/")
);
//#doIfEquals

//#doIfOrElse
doIfOrElse("#{condition}").then(
  http("name").get("/")
).orElse(
  http("else").get("/")
);
//#doIfOrElse

//#doIfEqualsOrElse
doIfEqualsOrElse("#{actual}", "expectedValue").then(
  // executed if the session value stored in "actual" equals to "expectedValue"
  http("name").get("/")
).orElse(
  // executed if the session value stored in "actual" is not equal to "expectedValue"
  http("else").get("/")
);
//#doIfEqualsOrElse

//#doSwitch
doSwitch("#{myKey}").on(
  onCase("foo").then(http("name1").get("/foo")),
  onCase("bar").then(http("name2").get("/bar"))
);
//#doSwitch

//#doSwitchOrElse
doSwitchOrElse("#{myKey}").on(
  onCase("foo").then(http("name1").get("/foo")),
  onCase("bar").then(http("name2").get("/bar"))
).orElse(
  http("name3").get("/baz")
);
//#doSwitchOrElse

//#randomSwitch
randomSwitch().on(
  percent(60.0).then(http("name1").get("/foo")),
  percent(40.0).then(http("name2").get("/bar"))
);
//#randomSwitch

//#randomSwitchOrElse
randomSwitchOrElse().on(
  percent(60.0).then(http("name1").get("/foo")),
  percent(20.0).then(http("name2").get("/bar"))
).orElse(
  http("name3").get("/baz")
);
//#randomSwitchOrElse

//#uniformRandomSwitch
uniformRandomSwitch().on(
  http("name1").get("/foo"),
  http("name2").get("/bar")
);
//#uniformRandomSwitch

//#roundRobinSwitch
roundRobinSwitch().on(
  http("name1").get("/foo"),
  http("name2").get("/bar")
);
//#roundRobinSwitch

//#tryMax
tryMax(5).on(
  http("name").get("/")
);

// with a counter name
tryMax(5, "counter").on(
  http("name").get("/")
);
//#tryMax

//#exitBlockOnFail
exitBlockOnFail().on(
  http("name").get("/")
);
//#exitBlockOnFail

//#exitHere
exitHere();
//#exitHere

//#exitHereIf
exitHereIf("#{myBoolean}");
exitHereIf(session -> true);
//#exitHereIf

//#exitHereIfFailed
exitHereIfFailed();
//#exitHereIfFailed

//#stopLoadGenerator
stopLoadGenerator("#{someErrorMessage}");
stopLoadGenerator(session -> "someErrorMessage");
crashLoadGenerator("#{someErrorMessage}");
crashLoadGenerator(session -> "someErrorMessage");
//#stopLoadGenerator

//#stopLoadGeneratorIf
stopLoadGeneratorIf("#{someErrorMessage}", "#{condition}");
stopLoadGeneratorIf(session -> "someErrorMessage", session -> true);
crashLoadGeneratorIf("#{someErrorMessage}", "#{condition}");
crashLoadGeneratorIf(session -> "someErrorMessage", session -> true);
//#stopLoadGeneratorIf

//#group
group("foo").on(
  exec(http("name").get("/"))
);
//#group
  }
}
