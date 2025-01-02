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

package io.gatling.javaapi.core;

import static io.gatling.javaapi.core.CoreDsl.*;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CoreJavaCompileTest extends Simulation {

  // execs
  private ChainBuilder chain1 = exec(session -> session);
  private ChainBuilder chain2 = exec(chain1, chain1).exec(chain1);
  // pauses
  private ChainBuilder pause1 = pause(1);
  // loops
  private ChainBuilder loop1 = repeat(1).on(chain1);
  // groups
  private ChainBuilder group1 = group("group").on(chain1);
  private ChainBuilder group2 = group(session -> "group").on(chain1);

  // bodies
  private Body stringBody1 = StringBody("static #{dynamic} static");
  private Body stringBody2 = StringBody(session -> "body");
  private Body rawFileBody1 = RawFileBody("path");
  private Body rawFileBody2 = RawFileBody(session -> "path");
  private Body elFileBody1 = ElFileBody("path");
  private Body elFileBody2 = ElFileBody(session -> "path");
  private Body pebbleStringBody = PebbleStringBody("template string");
  private Body pebbleFileBody1 = PebbleFileBody("path");
  private Body pebbleFileBody2 = PebbleFileBody(session -> "path");
  private Body byteArrayBody1 = ByteArrayBody(new byte[] {1});
  private Body byteArrayBody2 = ByteArrayBody(session -> new byte[] {1});
  private Body inputStreamBody =
      InputStreamBody(session -> new ByteArrayInputStream(new byte[] {1}));

  private List<Map<String, Object>> records = csv("foo").readRecords();
  private int recordsCount = csv("foo").recordsCount();

  // scenario
  private ScenarioBuilder scenario =
      scenario("scenario")
          // execs
          .exec(session -> session)
          .exec(chain1, chain2)
          .exec(Arrays.asList(chain1))
          // groups
          .group("group")
          .on(chain1, chain2)
          .group(session -> "group")
          .on(chain1, chain2)
          // feeds
          .feed(csv("foo"))
          .feed(csv("foo", '"'))
          .feed(ssv("foo"))
          .feed(ssv("foo", '"'))
          .feed(tsv("foo"))
          .feed(tsv("foo", '"'))
          .feed(separatedValues("foo", '|'))
          .feed(separatedValues("foo", '|', '"'))
          .feed(jsonFile("foo"))
          .feed(jsonUrl("foo"))
          .feed(
              Stream.iterate(0, i -> i + 1)
                  .limit(10)
                  .map(
                      i -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("key", i);
                        return map;
                      })
                  .iterator())
          .feed(
              () ->
                  Stream.iterate(0, i -> i + 1)
                      .limit(10)
                      .map(
                          i -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("key", i);
                            return map;
                          })
                      .iterator())
          .feed(arrayFeeder(new Map[] {new HashMap<>(), new HashMap<>()}).circular())
          .feed(listFeeder(Arrays.asList(new HashMap<>())).circular())
          // pauses
          .pause(1)
          .pause(Duration.ofMillis(100))
          .pause("#{pause}")
          .pause(session -> Duration.ofMillis(100))
          .pause(1, 2)
          .pause(Duration.ofMillis(100), Duration.ofMillis(200))
          .pause("#{min}", "#{max}")
          .pause(session -> Duration.ofMillis(100), session -> Duration.ofMillis(200))
          .pause(1, constantPauses)
          .pause(Duration.ofMillis(100), constantPauses)
          .pause("#{pause}", constantPauses)
          .pause(session -> Duration.ofMillis(100), constantPauses)
          .pause(1, 2, constantPauses)
          .pause(Duration.ofMillis(100), Duration.ofMillis(200), constantPauses)
          .pause("#{min}", "#{max}", constantPauses)
          .pause(
              session -> Duration.ofMillis(100), session -> Duration.ofMillis(200), constantPauses)
          .pause(1, disabledPauses)
          .pause(1, exponentialPauses)
          .pause(1, customPauses(session -> 1000L))
          .pause(1, uniformPausesPlusOrMinusPercentage(30))
          .pause(1, uniformPausesPlusOrMinusDuration(Duration.ofMillis((50))))
          .pause(1, normalPausesWithStdDevDuration(Duration.ofMillis(50)))
          .pause(1, normalPausesWithPercentageDuration(30))
          // pace
          .pace(1)
          .pace(1, "counter")
          .pace(Duration.ofSeconds(1))
          .pace(Duration.ofSeconds(1), "counter")
          .pace("#{pace}")
          .pace("#{pace}", "counter")
          .pace(session -> Duration.ofSeconds(1))
          .pace(session -> Duration.ofSeconds(1), "counter")
          .pace(1, 2)
          .pace(1, 2, "counter")
          .pace(Duration.ofSeconds(1), Duration.ofSeconds(2))
          .pace(Duration.ofSeconds(1), Duration.ofSeconds(2), "counter")
          .pace("#{min}", "#{max}", "counter")
          .pace(session -> Duration.ofSeconds(1), session -> Duration.ofSeconds(2))
          .pace(session -> Duration.ofSeconds(1), session -> Duration.ofSeconds(2), "counter")
          // rendezVous
          .rendezVous(5)
          // repeat
          .repeat(1)
          .on(chain1, chain2)
          .repeat(1, "counterName")
          .on(chain1, chain2)
          .repeat(session -> 1)
          .on(chain1, chain2)
          .repeat(session -> 1, "counterName")
          .on(chain1, chain2)
          // during
          .during(1)
          .on(chain1, chain2)
          .during(1, "counterName")
          .on(chain1, chain2)
          .during(1, true)
          .on(chain1, chain2)
          .during(1, "counterName", true)
          .on(chain1, chain2)
          .during(Duration.ofSeconds(1))
          .on(chain1, chain2)
          .during(Duration.ofSeconds(1), "counterName")
          .on(chain1, chain2)
          .during(Duration.ofSeconds(1), true)
          .on(chain1, chain2)
          .during(Duration.ofSeconds(1), "counterName", true)
          .on(chain1, chain2)
          .during("#{duration}")
          .on(chain1, chain2)
          .during("#{duration}", "counterName")
          .on(chain1, chain2)
          .during("#{duration}", true)
          .on(chain1, chain2)
          .during("#{duration}", "counterName", true)
          .on(chain1, chain2)
          .during(session -> Duration.ofSeconds(1))
          .on(chain1, chain2)
          .during(session -> Duration.ofSeconds(1), "counterName")
          .on(chain1, chain2)
          .during(session -> Duration.ofSeconds(1), true)
          .on(chain1, chain2)
          .during(session -> Duration.ofSeconds(1), "counterName", true)
          .on(chain1, chain2)
          // foreach
          .foreach(Arrays.asList(1), "attributeName")
          .on(chain1, chain2)
          .foreach(Arrays.asList(1), "attributeName", "counterName")
          .on(chain1, chain2)
          .foreach(session -> Arrays.asList(1), "attributeName")
          .on(chain1, chain2)
          .foreach(session -> Arrays.asList(1), "attributeName", "counterName")
          .on(chain1, chain2)
          // forever
          .forever()
          .on(chain1, chain2)
          .forever("counterName")
          .on(chain1, chain2)
          // asLongAs
          .asLongAs("#{condition}")
          .on(chain1, chain2)
          .asLongAs("#{condition}", "counterName")
          .on(chain1, chain2)
          .asLongAs("#{condition}", true)
          .on(chain1, chain2)
          .asLongAs("#{condition}", "counterName", true)
          .on(chain1, chain2)
          .asLongAs(session -> true)
          .on(chain1, chain2)
          .asLongAs(session -> true, "counterName")
          .on(chain1, chain2)
          .asLongAs(session -> true, true)
          .on(chain1, chain2)
          .asLongAs(session -> true, "counterName", true)
          .on(chain1, chain2)
          // doWhile
          .doWhile("#{condition}")
          .on(chain1, chain2)
          .doWhile("#{condition}", "counterName")
          .on(chain1, chain2)
          .doWhile(session -> true)
          .on(chain1, chain2)
          .doWhile(session -> true, "counterName")
          .on(chain1, chain2)
          // asLongAsDuring
          .asLongAsDuring("#{condition}", "#{duration}")
          .on(chain1, chain2)
          .asLongAsDuring("#{condition}", "#{duration}", "counterName")
          .on(chain1, chain2)
          .asLongAsDuring("#{condition}", "#{duration}", true)
          .on(chain1, chain2)
          .asLongAsDuring("#{condition}", "#{duration}", "counterName", true)
          .on(chain1, chain2)
          .asLongAsDuring(session -> true, session -> Duration.ofSeconds(1))
          .on(chain1, chain2)
          .asLongAsDuring(session -> true, session -> Duration.ofSeconds(1), "counterName")
          .on(chain1, chain2)
          .asLongAsDuring(session -> true, session -> Duration.ofSeconds(1), true)
          .on(chain1, chain2)
          .asLongAsDuring(session -> true, session -> Duration.ofSeconds(1), "counterName", true)
          .on(chain1, chain2)
          .doWhileDuring("#{condition}", "#{duration}")
          .on(chain1, chain2)
          .doWhileDuring("#{condition}", "#{duration}", "counterName")
          .on(chain1, chain2)
          .doWhileDuring("#{condition}", "#{duration}", true)
          .on(chain1, chain2)
          .doWhileDuring("#{condition}", "#{duration}", "counterName", true)
          .on(chain1, chain2)
          .doWhileDuring(session -> true, session -> Duration.ofSeconds(1))
          .on(chain1, chain2)
          .doWhileDuring(session -> true, session -> Duration.ofSeconds(1), "counterName")
          .on(chain1, chain2)
          .doWhileDuring(session -> true, session -> Duration.ofSeconds(1), true)
          .on(chain1, chain2)
          .doWhileDuring(session -> true, session -> Duration.ofSeconds(1), "counterName", true)
          .on(chain1, chain2)
          // doIf
          .doIf("#{condition}")
          .then(chain1, chain2)
          .doIf(session -> true)
          .then(chain1, chain2)
          // doIfOrElse
          .doIfOrElse("#{condition}")
          .then(chain1, chain2)
          .orElse(chain2, chain2)
          .doIfOrElse(session -> true)
          .then(chain1, chain2)
          .orElse(chain2, chain2)
          // doIfEquals
          .doIfEquals("#{actual}", 1)
          .then(chain1, chain2)
          .doIfEquals("#{actual}", "#{expected}")
          .then(chain1, chain2)
          .doIfEquals("#{actual}", session -> 1)
          .then(chain1, chain2)
          .doIfEquals(session -> "actual", 1)
          .then(chain1, chain2)
          .doIfEquals(session -> "actual", "#{expected}")
          .then(chain1, chain2)
          .doIfEquals(session -> "actual", session -> 1)
          .then(chain1, chain2)
          // doIfEqualsOrElse
          .doIfEqualsOrElse("#{actual}", 1)
          .then(chain1, chain2)
          .orElse(chain2, chain2)
          .doIfEqualsOrElse("#{actual}", "#{expected}")
          .then(chain1, chain2)
          .orElse(chain2, chain2)
          .doIfEqualsOrElse("#{actual}", session -> 1)
          .then(chain1, chain2)
          .orElse(chain2, chain2)
          .doIfEqualsOrElse(session -> "actual", 1)
          .then(chain1, chain2)
          .orElse(chain2, chain2)
          .doIfEqualsOrElse(session -> "actual", "#{expected}")
          .then(chain1, chain2)
          .orElse(chain2, chain2)
          .doIfEqualsOrElse(session -> "actual", session -> 1)
          .then(chain1, chain2)
          .orElse(chain2, chain2)
          // doSwitch
          .doSwitch("#{value}")
          .on(onCase("value1").then(chain1), onCase("value2").then(chain2))
          .doSwitch("#{value}")
          .on(Arrays.asList(onCase("value1").then(chain1)))
          .doSwitch(session -> "value")
          .on(onCase("value1").then(chain1), onCase("value2").then(chain2))
          .doSwitch(session -> "value")
          .on(Arrays.asList(onCase("value1").then(chain1)))
          // doSwitchOrElse
          .doSwitchOrElse("#{value}")
          .on(onCase("value1").then(chain1), onCase("value2").then(chain2))
          .orElse(chain2)
          .doSwitchOrElse("#{value}")
          .on(Arrays.asList(onCase("value1").then(chain1)))
          .orElse(chain2)
          .doSwitchOrElse(session -> "value")
          .on(onCase("value1").then(chain1), onCase("value2").then(chain2))
          .orElse(chain2)
          .doSwitchOrElse(session -> "value")
          .on(Arrays.asList(onCase("value1").then(chain1)))
          .orElse(chain2)
          // randomSwitch
          .randomSwitch()
          .on(percent(50.0).then(chain1), percent(50.0).then(chain2))
          .randomSwitch()
          .on(Arrays.asList(percent(50.0).then(chain1)))
          // randomSwitchOrElse
          .randomSwitchOrElse()
          .on(percent(50.0).then(chain1), percent(50.0).then(chain2))
          .orElse(chain2)
          .randomSwitchOrElse()
          .on(Arrays.asList(percent(50.0).then(chain1)))
          .orElse(chain2)
          // uniformRandomSwitch
          .uniformRandomSwitch()
          .on(chain1, chain2)
          .uniformRandomSwitch()
          .on(Arrays.asList(chain1))
          // roundRobinSwitch
          .roundRobinSwitch()
          .on(chain1, chain2)
          .roundRobinSwitch()
          .on(Arrays.asList(chain1))
          // exitBlockOnFail
          .exitBlockOnFail()
          .on(chain1)
          // tryMax
          .tryMax(1)
          .on(chain1)
          .tryMax(1, "counterName")
          .on(chain1)
          .tryMax("#{times}")
          .on(chain1)
          .tryMax("#{times}", "counterName")
          .on(chain1)
          .tryMax(session -> 1)
          .on(chain1)
          .tryMax(session -> 1, "counterName")
          .on(chain1)
          // exitHereIf
          .exitHereIf("#{condition}")
          .exitHereIf(session -> true)
          // exitHere
          .exitHere()
          // exitHereIfFailed
          .exitHereIfFailed()
          // stopLoadGenerator
          .stopLoadGenerator("#{message}")
          .stopLoadGenerator(session -> "message")
          // stopLoadGeneratorIf
          .stopLoadGeneratorIf("#{message}", "#{condition}")
          .stopLoadGeneratorIf(session -> "message", session -> true)
          .stopLoadGeneratorIf("#{message}", session -> true)
          .stopLoadGeneratorIf(session -> "message", "#{condition}");

  {
    registerPebbleExtensions((io.pebbletemplates.pebble.extension.Extension) null);
    setUp(
            scenario.injectOpen(
                rampUsers(5).during(1),
                rampUsers(5).during(Duration.ofSeconds(1)),
                stressPeakUsers(5).during(1),
                stressPeakUsers(5).during(Duration.ofSeconds(1)),
                atOnceUsers(1000),
                constantUsersPerSec(10).during(1),
                constantUsersPerSec(10).during(Duration.ofSeconds(1)),
                rampUsersPerSec(100).to(200).during(1),
                rampUsersPerSec(100).to(200).during(Duration.ofSeconds(1)),
                nothingFor(1),
                nothingFor(Duration.ofSeconds(1)),
                incrementUsersPerSec(1.0).times(5).eachLevelLasting(1),
                incrementUsersPerSec(1.0).times(5).eachLevelLasting(1).startingFrom(1.0),
                incrementUsersPerSec(1.0).times(5).eachLevelLasting(1).separatedByRampsLasting(1),
                incrementUsersPerSec(1.0)
                    .times(5)
                    .eachLevelLasting(1)
                    .startingFrom(1.0)
                    .separatedByRampsLasting(1),
                incrementUsersPerSec(1.0)
                    .times(5)
                    .eachLevelLasting(Duration.ofSeconds(1))
                    .startingFrom(1.0)
                    .separatedByRampsLasting(Duration.ofSeconds(1))),
            scenario
                .injectClosed(
                    constantConcurrentUsers(100).during(1),
                    constantConcurrentUsers(100).during(Duration.ofSeconds(1)),
                    rampConcurrentUsers(1).to(5).during(1),
                    rampConcurrentUsers(1).to(5).during(Duration.ofSeconds(1)),
                    incrementConcurrentUsers(1).times(5).eachLevelLasting(1),
                    incrementConcurrentUsers(1).times(5).eachLevelLasting(1),
                    incrementConcurrentUsers(1).times(5).eachLevelLasting(1).startingFrom(1),
                    incrementConcurrentUsers(1)
                        .times(5)
                        .eachLevelLasting(1)
                        .separatedByRampsLasting(1),
                    incrementConcurrentUsers(1)
                        .times(5)
                        .eachLevelLasting(1)
                        .startingFrom(1)
                        .separatedByRampsLasting(1),
                    incrementConcurrentUsers(1)
                        .times(5)
                        .eachLevelLasting(Duration.ofSeconds(1))
                        .startingFrom(1)
                        .separatedByRampsLasting(Duration.ofSeconds(1)))
                .andThen(scenario.injectOpen(atOnceUsers(1))))
        .assertions(
            global().allRequests().count().is(5L),
            global().allRequests().percent().is(5.5),
            forAll().allRequests().count().is(5L),
            details("group", "request").allRequests().count().is(5L))
        .maxDuration(1)
        .maxDuration(Duration.ofSeconds(1))
        .throttle(
            reachRps(100).in(1),
            reachRps(100).in(Duration.ofSeconds(1)),
            jumpToRps(100),
            holdFor(1),
            holdFor(Duration.ofSeconds(1)))
        .disablePauses()
        .constantPauses()
        .exponentialPauses()
        .customPauses(session -> 1L)
        .uniformPauses(1)
        .uniformPauses(Duration.ofSeconds(1))
        .normalPausesWithStdDevDuration(Duration.ofMillis(50))
        .normalPausesWithPercentageDuration(30);

    int indexOfLoadGeneratorInRun = deploymentInfo.indexOfLoadGeneratorInRun;
  }
}
