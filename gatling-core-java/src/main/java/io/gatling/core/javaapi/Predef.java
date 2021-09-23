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

package io.gatling.core.javaapi;

import com.mitchellbosecke.pebble.extension.Extension;
import io.gatling.commons.stats.assertion.Details;
import io.gatling.commons.stats.assertion.ForAll$;
import io.gatling.commons.stats.assertion.Global$;
import io.gatling.core.Predef$;
import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.core.body.BodyProcessors;
import io.gatling.core.feeder.BatchableFeederBuilder;
import io.gatling.core.feeder.FeederBuilderBase;
import io.gatling.core.feeder.FileBasedFeederBuilder;
import io.gatling.core.feeder.SeparatedValuesParser$;
import io.gatling.core.javaapi.internal.CoreCheckType;
import io.gatling.core.javaapi.internal.ScalaHelpers;
import io.gatling.core.javaapi.internal.StructureBuilder;
import io.gatling.core.javaapi.internal.condition.*;
import io.gatling.core.javaapi.internal.error.Errors;
import io.gatling.core.javaapi.internal.group.Groups;
import io.gatling.core.javaapi.internal.loop.*;
import scala.collection.immutable.Seq;

import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

@SuppressWarnings("unused")
public final class Predef {

  private Predef() {
  }

  ////////// CoreDsl
  public static ScenarioBuilder scenario(String name) {
    return new ScenarioBuilder(name);
  }

  public static Filter.AllowList AllowList(String... patterns) {
    return AllowList(Arrays.asList(patterns));
  }

  public static Filter.AllowList AllowList(List<String> patterns) {
    return new Filter.AllowList(patterns);
  }

  public static Filter.DenyList DenyList(String... patterns) {
    return DenyList(Arrays.asList(patterns));
  }

  public static Filter.DenyList DenyList(List<String> patterns) {
    return new Filter.DenyList(patterns);
  }

  public static Function<Session, Session> flattenMapIntoAttributes(String map) {
    return session -> new Session(toJavaFunction(io.gatling.core.Predef.flattenMapIntoAttributes(toMapExpression(map))).apply(session));
  }

  ////////// StructureBuilder.Execs
  public static ChainBuilder exec(Function<Session, Session> f) {
    return ChainBuilder.EMPTY.exec(f);
  }

  public static ChainBuilder exec(ActionBuilder actionBuilder) {
    return ChainBuilder.EMPTY.exec(actionBuilder);
  }

  public static ChainBuilder exec(StructureBuilder<?, ?>... structureBuilders) {
    return ChainBuilder.EMPTY.exec(structureBuilders);
  }

  public static ChainBuilder exec(List<StructureBuilder<?, ?>> structureBuilders) {
    return ChainBuilder.EMPTY.exec(structureBuilders);
  }

  ////////// StructureBuilder.Pauses
  public static ChainBuilder pause(long duration) {
    return ChainBuilder.EMPTY.pause(duration);
  }

  public static ChainBuilder pause(long duration, PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(duration, pauseType);
  }

  public static ChainBuilder pause(Duration duration) {
    return ChainBuilder.EMPTY.pause(duration);
  }

  public static ChainBuilder pause(Duration duration, PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(duration, pauseType);
  }

  public static ChainBuilder pause(String duration) {
    return ChainBuilder.EMPTY.pause(duration);
  }

  public static ChainBuilder pause(String duration, PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(duration, pauseType);
  }

  public static ChainBuilder pause(Function<Session, Duration> f) {
    return ChainBuilder.EMPTY.pause(f);
  }

  public static ChainBuilder pause(Function<Session, Duration> f, PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(f, pauseType);
  }

  public static ChainBuilder pause(long min, long max) {
    return ChainBuilder.EMPTY.pause(min, max);
  }

  public static ChainBuilder pause(Duration min, Duration max) {
    return ChainBuilder.EMPTY.pause(min, max);
  }

  public static ChainBuilder pause(Duration min, Duration max, PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(min, max, pauseType);
  }

  public static ChainBuilder pause(String min, String max) {
    return ChainBuilder.EMPTY.pause(min, max);
  }

  public static ChainBuilder pause(String min, String max, PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(min, max, pauseType);
  }

  public static ChainBuilder pause(Function<Session, Duration> min, Function<Session, Duration> max) {
    return ChainBuilder.EMPTY.pause(min, max);
  }

  public static ChainBuilder pause(Function<Session, Duration> min, Function<Session, Duration> max, PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(min, max, pauseType);
  }

  public static ChainBuilder pace(long duration) {
    return ChainBuilder.EMPTY.pace(duration);
  }

  public static ChainBuilder pace(long duration, String counterName) {
    return ChainBuilder.EMPTY.pace(duration, counterName);
  }

  public static ChainBuilder pace(Duration duration) {
    return ChainBuilder.EMPTY.pace(duration);
  }

  public static ChainBuilder pace(Duration duration, String counterName) {
    return ChainBuilder.EMPTY.pace(duration, counterName);
  }

  public static ChainBuilder pace(String duration) {
    return ChainBuilder.EMPTY.pace(duration);
  }

  public static ChainBuilder pace(String duration, String counterName) {
    return ChainBuilder.EMPTY.pace(duration, counterName);
  }

  public static ChainBuilder pace(Function<Session, Duration> duration) {
    return ChainBuilder.EMPTY.pace(duration);
  }

  public static ChainBuilder pace(Function<Session, Duration> duration, String counterName) {
    return ChainBuilder.EMPTY.pace(duration, counterName);
  }

  public static ChainBuilder pace(long min, long max) {
    return ChainBuilder.EMPTY.pace(min, max);
  }

  public static ChainBuilder pace(long min, long max, String counterName) {
    return ChainBuilder.EMPTY.pace(min, max, counterName);
  }

  public static ChainBuilder pace(Duration min, Duration max) {
    return ChainBuilder.EMPTY.pace(min, max);
  }

  public static ChainBuilder pace(Duration min, Duration max, String counterName) {
    return ChainBuilder.EMPTY.pace(min, max, counterName);
  }

  public static ChainBuilder pace(String min, String max, String counterName) {
    return ChainBuilder.EMPTY.pace(min, max, counterName);
  }

  public static ChainBuilder pace(Function<Session, Duration> min, Function<Session, Duration> max) {
    return ChainBuilder.EMPTY.pace(min, max);
  }

  public static ChainBuilder pace(Function<Session, Duration> min, Function<Session, Duration> max, String counterName) {
    return ChainBuilder.EMPTY.pace(min, max, counterName);
  }

  public static ChainBuilder rendezVous(int users) {
    return ChainBuilder.EMPTY.rendezVous(users);
  }

  ////////// StructureBuilder.Feeds
  public static ChainBuilder feed(Supplier<Map<String, Object>> feederBuilder) {
    return ChainBuilder.EMPTY.feed(feederBuilder);
  }

  public static ChainBuilder feed(Iterator<Map<String, Object>> feeder) {
    return ChainBuilder.EMPTY.feed(feeder);
  }

  public static ChainBuilder feed(FeederBuilderBase<?> feederBuilder) {
    return ChainBuilder.EMPTY.feed(feederBuilder);
  }

  ////////// StructureBuilder.Loops
  public static Repeat.Loop<ChainBuilder> repeat(int times) {
    return ChainBuilder.EMPTY.repeat(times);
  }

  public static Repeat.Loop<ChainBuilder> repeat(int times, String counterName) {
    return ChainBuilder.EMPTY.repeat(times, counterName);
  }

  public static Repeat.Loop<ChainBuilder> repeat(String times) {
    return ChainBuilder.EMPTY.repeat(times);
  }

  public static Repeat.Loop<ChainBuilder> repeat(String times, String counterName) {
    return ChainBuilder.EMPTY.repeat(times, counterName);
  }

  public static Repeat.Loop<ChainBuilder> repeat(Function<Session, Integer> times) {
    return ChainBuilder.EMPTY.repeat(times);
  }

  public static Repeat.Loop<ChainBuilder> repeat(Function<Session, Integer> times, String counterName) {
    return ChainBuilder.EMPTY.repeat(times, counterName);
  }

  public static ForEach.Loop<ChainBuilder> foreach(List<Object> seq, String attributeName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName);
  }

  public static ForEach.Loop<ChainBuilder> foreach(List<Object> seq, String attributeName, String counterName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName, counterName);
  }

  public static ForEach.Loop<ChainBuilder> foreach(Function<Session, List<Object>> seq, String attributeName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName);
  }

  public static ForEach.Loop<ChainBuilder> foreach(Function<Session, List<Object>> seq, String attributeName, String counterName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName, counterName);
  }

  public static During.Loop<ChainBuilder> during(long duration) {
    return ChainBuilder.EMPTY.during(duration);
  }

  public static During.Loop<ChainBuilder> during(long duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, exitASAP);
  }

  public static During.Loop<ChainBuilder> during(long duration, String counterName) {
    return ChainBuilder.EMPTY.during(duration, counterName);
  }

  public static During.Loop<ChainBuilder> during(long duration, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, counterName, exitASAP);
  }

  public static During.Loop<ChainBuilder> during(Duration duration) {
    return ChainBuilder.EMPTY.during(duration);
  }

  public static During.Loop<ChainBuilder> during(Duration duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, exitASAP);
  }

  public static During.Loop<ChainBuilder> during(Duration duration, String counterName) {
    return ChainBuilder.EMPTY.during(duration, counterName);
  }

  public static During.Loop<ChainBuilder> during(Duration duration, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, counterName, exitASAP);
  }

  public static During.Loop<ChainBuilder> during(String duration) {
    return ChainBuilder.EMPTY.during(duration);
  }

  public static During.Loop<ChainBuilder> during(String duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, exitASAP);
  }

  public static During.Loop<ChainBuilder> during(String duration, String counterName) {
    return ChainBuilder.EMPTY.during(duration, counterName);
  }

  public static During.Loop<ChainBuilder> during(String duration, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, counterName, exitASAP);
  }

  public static During.Loop<ChainBuilder> during(Function<Session, Duration> duration) {
    return ChainBuilder.EMPTY.during(duration);
  }

  public static During.Loop<ChainBuilder> during(Function<Session, Duration> duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, exitASAP);
  }

  public static During.Loop<ChainBuilder> during(Function<Session, Duration> duration, String counterName) {
    return ChainBuilder.EMPTY.during(duration, counterName);
  }

  public static During.Loop<ChainBuilder> during(Function<Session, Duration> duration, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, counterName, exitASAP);
  }

  public static Forever.Loop<ChainBuilder> forever() {
    return ChainBuilder.EMPTY.forever();
  }

  public static Forever.Loop<ChainBuilder> forever(String counterName) {
    return ChainBuilder.EMPTY.forever(counterName);
  }

  public static Forever.Loop<ChainBuilder> forever(boolean exitASAP) {
    return ChainBuilder.EMPTY.forever(exitASAP);
  }

  public static Forever.Loop<ChainBuilder> forever(String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.forever(counterName, exitASAP);
  }

  public static AsLongAs.Loop<ChainBuilder> asLongAs(String condition) {
    return ChainBuilder.EMPTY.asLongAs(condition);
  }

  public static AsLongAs.Loop<ChainBuilder> asLongAs(String condition, String counterName) {
    return ChainBuilder.EMPTY.asLongAs(condition, counterName);
  }

  public static AsLongAs.Loop<ChainBuilder> asLongAs(String condition, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAs(condition, exitASAP);
  }

  public static AsLongAs.Loop<ChainBuilder> asLongAs(String condition, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAs(condition, counterName, exitASAP);
  }

  public static AsLongAs.Loop<ChainBuilder> asLongAs(Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.asLongAs(condition);
  }

  public static AsLongAs.Loop<ChainBuilder> asLongAs(Function<Session, Boolean> condition, String counterName) {
    return ChainBuilder.EMPTY.asLongAs(condition, counterName);
  }

  public static AsLongAs.Loop<ChainBuilder> asLongAs(Function<Session, Boolean> condition, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAs(condition, exitASAP);
  }

  public static AsLongAs.Loop<ChainBuilder> asLongAs(Function<Session, Boolean> condition, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAs(condition, counterName, exitASAP);
  }

  public static DoWhile.Loop<ChainBuilder> doWhile(String condition) {
    return ChainBuilder.EMPTY.doWhile(condition);
  }

  public static DoWhile.Loop<ChainBuilder> doWhile(String condition, String counterName) {
    return ChainBuilder.EMPTY.doWhile(condition, counterName);
  }

  public static DoWhile.Loop<ChainBuilder> doWhile(Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.doWhile(condition);
  }

  public static DoWhile.Loop<ChainBuilder> doWhile(Function<Session, Boolean> condition, String counterName) {
    return ChainBuilder.EMPTY.doWhile(condition, counterName);
  }

  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(String condition, String duration) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration);
  }

  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(String condition, String duration, String counterName) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, counterName);
  }

  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(String condition, String duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, exitASAP);
  }

  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(String condition, String duration, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, counterName, exitASAP);
  }

  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration);
  }

  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, String counterName) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, counterName);
  }

  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, exitASAP);
  }

  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, counterName, exitASAP);
  }

  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(String condition, String duration) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration);
  }

  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(String condition, String duration, String counterName) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, counterName);
  }

  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(String condition, String duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, exitASAP);
  }

  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(String condition, String duration, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, counterName, exitASAP);
  }

  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration);
  }

  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, String counterName) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, counterName);
  }

  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, exitASAP);
  }

  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, counterName, exitASAP);
  }

  ////////// StructureBuilder.ConditionalStatements
  public static DoIf.Then<ChainBuilder> doIf(String condition) {
    return ChainBuilder.EMPTY.doIf(condition);
  }

  public static DoIf.Then<ChainBuilder> doIf(Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.doIf(condition);
  }

  public static DoIfOrElse.Then<ChainBuilder> doIfOrElse(String condition) {
    return ChainBuilder.EMPTY.doIfOrElse(condition);
  }

  public static DoIfOrElse.Then<ChainBuilder> doIfOrElse(Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.doIfOrElse(condition);
  }

  public static DoIfEquals.Then<ChainBuilder> doIfEquals(String actual, String expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  public static DoIfEquals.Then<ChainBuilder> doIfEquals(String actual, Object expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  public static DoIfEquals.Then<ChainBuilder> doIfEquals(String actual, Function<Session, Object> expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  public static DoIfEquals.Then<ChainBuilder> doIfEquals(Function<Session, Object> actual, String expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  public static DoIfEquals.Then<ChainBuilder> doIfEquals(Function<Session, Object> actual, Object expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  public static DoIfEquals.Then<ChainBuilder> doIfEquals(Function<Session, Object> actual, Function<Session, Object> expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(String actual, String expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(String actual, Object expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(String actual, Function<Session, Object> expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(Function<Session, Object> actual, String expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(Function<Session, Object> actual, Object expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(Function<Session, Object> actual, Function<Session, Object> expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  public static DoSwitch.Possibilities<ChainBuilder> doSwitch(String value) {
    return ChainBuilder.EMPTY.doSwitch(value);
  }

  public static DoSwitch.Possibilities<ChainBuilder> doSwitch(Function<Session, Object> value) {
    return ChainBuilder.EMPTY.doSwitch(value);
  }

  public static DoSwitchOrElse.Possibilities<ChainBuilder> doSwitchOrElse(String value) {
    return ChainBuilder.EMPTY.doSwitchOrElse(value);
  }

  public static DoSwitchOrElse.Possibilities<ChainBuilder> doSwitchOrElse(Function<Session, Object> value) {
    return ChainBuilder.EMPTY.doSwitchOrElse(value);
  }

  public static ChainBuilder randomSwitch(RandomSwitchPossibility... possibilities) {
    return ChainBuilder.EMPTY.randomSwitch(possibilities);
  }

  public static ChainBuilder randomSwitch(List<RandomSwitchPossibility> possibilities) {
    return ChainBuilder.EMPTY.randomSwitch(possibilities);
  }

  public static RandomSwitchOrElse.OrElse<ChainBuilder> randomSwitchOrElse(RandomSwitchPossibility... possibilities) {
    return ChainBuilder.EMPTY.randomSwitchOrElse(possibilities);
  }

  public static RandomSwitchOrElse.OrElse<ChainBuilder> randomSwitchOrElse(List<RandomSwitchPossibility> possibilities) {
    return ChainBuilder.EMPTY.randomSwitchOrElse(possibilities);
  }

  public static ChainBuilder uniformRandomSwitch(ChainBuilder... possibilities) {
    return ChainBuilder.EMPTY.uniformRandomSwitch(possibilities);
  }

  public static ChainBuilder uniformRandomSwitch(List<ChainBuilder> possibilities) {
    return ChainBuilder.EMPTY.uniformRandomSwitch(possibilities);
  }

  public static ChainBuilder roundRobinSwitch(ChainBuilder... possibilities) {
    return ChainBuilder.EMPTY.roundRobinSwitch(possibilities);
  }

  public static ChainBuilder roundRobinSwitch(List<ChainBuilder> possibilities) {
    return ChainBuilder.EMPTY.roundRobinSwitch(possibilities);
  }

  ////////// StructureBuilder.Errors
  public static ChainBuilder exitBlockOnFail(ChainBuilder chain) {
    return ChainBuilder.EMPTY.exitBlockOnFail(chain);
  }

  public static Errors.TryMax<ChainBuilder> tryMax(int times) {
    return ChainBuilder.EMPTY.tryMax(times);
  }

  public static Errors.TryMax<ChainBuilder> tryMax(int times, String counterName) {
    return ChainBuilder.EMPTY.tryMax(times, counterName);
  }

  public static Errors.TryMax<ChainBuilder> tryMax(String times) {
    return ChainBuilder.EMPTY.tryMax(times);
  }

  public static Errors.TryMax<ChainBuilder> tryMax(String times, String counterName) {
    return ChainBuilder.EMPTY.tryMax(times, counterName);
  }

  public static Errors.TryMax<ChainBuilder> tryMax(Function<Session, Integer> times) {
    return ChainBuilder.EMPTY.tryMax(times);
  }

  public static Errors.TryMax<ChainBuilder> tryMax(Function<Session, Integer> times, String counterName) {
    return ChainBuilder.EMPTY.tryMax(times, counterName);
  }

  public static ChainBuilder exitHereIf(String condition) {
    return ChainBuilder.EMPTY.exitHereIf(condition);
  }

  public static ChainBuilder exitHereIf(Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.exitHereIf(condition);
  }

  public static ChainBuilder exitHere() {
    return ChainBuilder.EMPTY.exitHere();
  }

  public static ChainBuilder exitHereIfFailed() {
    return ChainBuilder.EMPTY.exitHereIfFailed();
  }

  ////////// StructureBuilder.Groups
  public static Groups.Grouping<ChainBuilder> group(String name) {
    return ChainBuilder.EMPTY.group(name);
  }

  public static Groups.Grouping<ChainBuilder> group(Function<Session, String> name) {
    return ChainBuilder.EMPTY.group(name);
  }

  ////////// PauseSupport
  public static PauseType disabledPauses = PauseType.Disabled;
  public static PauseType constantPauses = PauseType.Constant;
  public static PauseType exponentialPauses = PauseType.Exponential;

  public static PauseType normalPausesWithPercentageDuration(double stdDev) {
    return new PauseType.NormalWithPercentageDuration(stdDev);
  }

  public static PauseType normalPausesWithStdDevDuration(Duration stdDev) {
    return new PauseType.NormalWithStdDevDuration(stdDev);
  }

  public static PauseType customPauses(Function<Session, Long> f) {
    return new PauseType.Custom(toUntypedGatlingSessionFunction(f));
  }

  public static PauseType uniformPausesPlusOrMinusPercentage(double plusOrMinus) {
    return new PauseType.UniformPercentage(plusOrMinus);
  }

  public static PauseType uniformPausesPlusOrMinusDuration(Duration plusOrMinus) {
    return new PauseType.UniformDuration(plusOrMinus);
  }

  ////////// CheckSupport
  // Couldn't manage to implement free checkIf

  public static CheckBuilder.Find<String> bodyString() {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.bodyString(), CoreCheckType.BodyString, Function.identity());
  }
  public static CheckBuilder.Find<byte[]> bodyBytes() {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.bodyBytes(), CoreCheckType.BodyBytes, Function.identity());
  }

  public static CheckBuilder.Find<Integer> bodyLength() {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.bodyLength(), CoreCheckType.BodyLength, Integer.class::cast);
  }

  public static CheckBuilder.Find<InputStream> bodyStream() {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.bodyStream(), CoreCheckType.BodyStream, Function.identity());
  }

  public static CheckBuilder.Find<Integer> substring(String pattern) {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.substring(toStringExpression(pattern)), CoreCheckType.Substring, Integer.class::cast);
  }

  public static CheckBuilder.Find<Integer> substring(Function<Session, String> pattern) {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.substring(toTypedGatlingSessionFunction(pattern)), CoreCheckType.Substring, Integer.class::cast);
  }

  public static CheckBuilder.MultipleFind<String> xpath(String path) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.xpath(toStringExpression(path), Predef$.MODULE$.defaultXmlParsers()), CoreCheckType.XPath, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> xpath(Function<Session, String> path) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.xpath(toTypedGatlingSessionFunction(path), Predef$.MODULE$.defaultXmlParsers()), CoreCheckType.XPath, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> xpath(String path, Map<String, String> namespaces) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.xpath(toStringExpression(path), toScalaMap(namespaces), Predef$.MODULE$.defaultXmlParsers()), CoreCheckType.XPath, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> xpath(Function<Session, String> path, Map<String, String> namespaces) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.xpath(toTypedGatlingSessionFunction(path), toScalaMap(namespaces), Predef$.MODULE$.defaultXmlParsers()), CoreCheckType.XPath, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> css(String selector) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.css(toStringExpression(selector), Predef$.MODULE$.defaultCssSelectors()), CoreCheckType.Css, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> css(Function<Session, String> selector) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.css(toTypedGatlingSessionFunction(selector), Predef$.MODULE$.defaultCssSelectors()), CoreCheckType.Css, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> css(String selector, String nodeAttribute) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.css(toStringExpression(selector), nodeAttribute, Predef$.MODULE$.defaultCssSelectors()), CoreCheckType.Css, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> css(Function<Session, String> selector, String nodeAttribute) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.css(toTypedGatlingSessionFunction(selector), nodeAttribute, Predef$.MODULE$.defaultCssSelectors()), CoreCheckType.Css, Function.identity());
  }

  public static CheckBuilder.MultipleFind<Map<String, Object>> form(String selector) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.form(toStringExpression(selector), Predef$.MODULE$.defaultCssSelectors()), CoreCheckType.Css, ScalaHelpers::toJavaMap);
  }

  public static CheckBuilder.MultipleFind<Map<String, Object>> form(Function<Session, String> selector) {
    return new CheckBuilder.MultipleFind.Default<>(Predef$.MODULE$.form(toTypedGatlingSessionFunction(selector), Predef$.MODULE$.defaultCssSelectors()), CoreCheckType.Css, ScalaHelpers::toJavaMap);
  }

  public static CheckBuilder.JsonOfTypeMultipleFind jsonPath(String path) {
    return new CheckBuilder.JsonPath(Predef$.MODULE$.jsonPath(toStringExpression(path), Predef$.MODULE$.defaultJsonPaths()));
  }

  public static CheckBuilder.JsonOfTypeMultipleFind jsonPath(Function<Session, String> path) {
    return new CheckBuilder.JsonPath(Predef$.MODULE$.jsonPath(toTypedGatlingSessionFunction(path), Predef$.MODULE$.defaultJsonPaths()));
  }

  public static CheckBuilder.JsonOfTypeFind jmesPath(String path) {
    return new CheckBuilder.JmesPath(Predef$.MODULE$.jmesPath(toStringExpression(path), Predef$.MODULE$.defaultJmesPaths()));
  }

  public static CheckBuilder.JsonOfTypeFind jmesPath(Function<Session, String> path) {
    return new CheckBuilder.JmesPath(Predef$.MODULE$.jmesPath(toTypedGatlingSessionFunction(path), Predef$.MODULE$.defaultJmesPaths()));
  }

  public static CheckBuilder.JsonOfTypeMultipleFind jsonpJsonPath(String path) {
    return new CheckBuilder.JsonpJsonPath(Predef$.MODULE$.jsonpJsonPath(toStringExpression(path), Predef$.MODULE$.defaultJsonPaths()));
  }

  public static CheckBuilder.JsonOfTypeMultipleFind jsonpJsonPath(Function<Session, String> path) {
    return new CheckBuilder.JsonpJsonPath(Predef$.MODULE$.jsonpJsonPath(toTypedGatlingSessionFunction(path), Predef$.MODULE$.defaultJsonPaths()));
  }

  public static CheckBuilder.JsonOfTypeFind jsonpJmesPath(String path) {
    return new CheckBuilder.JsonpJmesPath(Predef$.MODULE$.jsonpJmesPath(toStringExpression(path), Predef$.MODULE$.defaultJmesPaths()));
  }

  public static CheckBuilder.JsonOfTypeFind jsonpJmesPath(Function<Session, String> path) {
    return new CheckBuilder.JsonpJmesPath(Predef$.MODULE$.jsonpJmesPath(toTypedGatlingSessionFunction(path), Predef$.MODULE$.defaultJmesPaths()));
  }

  public static CheckBuilder.CaptureGroupCheckBuilder regex(String pattern) {
    return new CheckBuilder.Regex(Predef$.MODULE$.regex(toStringExpression(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static CheckBuilder.CaptureGroupCheckBuilder regex(Function<Session, String> pattern) {
    return new CheckBuilder.Regex(Predef$.MODULE$.regex(toTypedGatlingSessionFunction(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static void registerJmesPathFunctions(io.burt.jmespath.function.Function... functions) {
    Predef$.MODULE$.registerJmesPathFunctions(toScalaSeq(functions));
  }

  public static CheckBuilder.Find<String> md5() {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.md5(), CoreCheckType.Md5, Function.identity());
  }

  public static CheckBuilder.Find<String> sha1() {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.sha1(), CoreCheckType.Sha1, Function.identity());
  }

  public static CheckBuilder.Find<Integer> responseTimeInMillis() {
    return new CheckBuilder.Find.Default<>(Predef$.MODULE$.responseTimeInMillis(), CoreCheckType.ResponseTime, Integer.class::cast);
  }

  ////////// FeederSupport
  public static BatchableFeederBuilder<String> csv(String fileName) {
    return csv(fileName, SeparatedValuesParser$.MODULE$.DefaultQuoteChar());
  }

  public static BatchableFeederBuilder<String> csv(String fileName, char quoteChar) {
    return Predef$.MODULE$.csv(fileName, quoteChar, Predef$.MODULE$.configuration());
  }

  public static BatchableFeederBuilder<String> ssv(String fileName) {
    return ssv(fileName, SeparatedValuesParser$.MODULE$.DefaultQuoteChar());
  }

  public static BatchableFeederBuilder<String> ssv(String fileName, char quoteChar) {
    return Predef$.MODULE$.ssv(fileName, quoteChar, Predef$.MODULE$.configuration());
  }

  public static BatchableFeederBuilder<String> tsv(String fileName) {
    return tsv(fileName, SeparatedValuesParser$.MODULE$.DefaultQuoteChar());
  }

  public static BatchableFeederBuilder<String> tsv(String fileName, char quoteChar) {
    return Predef$.MODULE$.tsv(fileName, quoteChar, Predef$.MODULE$.configuration());
  }

  public static BatchableFeederBuilder<String> separatedValues(String fileName, char separator) {
    return separatedValues(fileName, separator, SeparatedValuesParser$.MODULE$.DefaultQuoteChar());
  }

  public static BatchableFeederBuilder<String> separatedValues(String fileName, char separator, char quoteChar) {
    return Predef$.MODULE$.separatedValues(fileName, separator, quoteChar, Predef$.MODULE$.configuration());
  }

  public static FileBasedFeederBuilder<Object> jsonFile(String fileName) {
    return Predef$.MODULE$.jsonFile(fileName, Predef$.MODULE$.defaultJsonParsers(), Predef$.MODULE$.configuration());
  }

  public static FeederBuilderBase<Object> jsonUrl(String url) {
    return Predef$.MODULE$.jsonUrl(url, Predef$.MODULE$.defaultJsonParsers(), Predef$.MODULE$.configuration());
  }

  @SuppressWarnings("unchecked")
  public static FeederBuilderBase<Object> arrayFeeder(Map<String, Object>[] data) {
    scala.collection.immutable.Map<String, Object>[] scalaArray = Arrays.stream(data).map(ScalaHelpers::toScalaMap).toArray(scala.collection.immutable.Map[]::new);
    return Predef$.MODULE$.array2FeederBuilder(scalaArray, Predef$.MODULE$.configuration());
  }

  public static FeederBuilderBase<Object> listFeeder(List<Map<String, Object>> data) {
    scala.collection.immutable.Seq<scala.collection.immutable.Map<String, Object>> seq = toScalaSeq(data.stream().map(ScalaHelpers::toScalaMap).collect(Collectors.toList()));
    return Predef$.MODULE$.seq2FeederBuilder(seq.toIndexedSeq(), Predef$.MODULE$.configuration());
  }

  //////////  OpenInjectionSupport
  public static OpenInjectionStep.RampBuilder rampUsers(int users) {
    return new OpenInjectionStep.RampBuilder(users);
  }

  public static OpenInjectionStep.HeavisideBuilder heavisideUsers(int users) {
    return new OpenInjectionStep.HeavisideBuilder(users);
  }

  public static OpenInjectionStep atOnceUsers(int users) {
    return new OpenInjectionStep(new io.gatling.core.controller.inject.open.AtOnceOpenInjection(users));
  }

  public static OpenInjectionStep.ConstantRateBuilder constantUsersPerSec(double rate) {
    return new OpenInjectionStep.ConstantRateBuilder(rate);
  }

  public static OpenInjectionStep.PartialRampRateBuilder rampUsersPerSec(double rate) {
    return new OpenInjectionStep.PartialRampRateBuilder(rate);
  }

  public static OpenInjectionStep nothingFor(int durationSeconds) {
    return nothingFor(Duration.ofSeconds(durationSeconds));
  }

  public static OpenInjectionStep nothingFor(Duration duration) {
    return new OpenInjectionStep(new io.gatling.core.controller.inject.open.NothingForOpenInjection(toScalaDuration(duration)));
  }

  public static OpenInjectionStep.IncreasingUsersPerSecProfileBuilder incrementUsersPerSec(double usersPerSec) {
    return new OpenInjectionStep.IncreasingUsersPerSecProfileBuilder(usersPerSec);
  }

  //////////  ClosedInjectionSupport
  public static ClosedInjectionStep.ConstantConcurrentUsersBuilder constantConcurrentUsers(int number) {
    return new ClosedInjectionStep.ConstantConcurrentUsersBuilder(number);
  }

  public static ClosedInjectionStep.RampConcurrentUsersInjectionFrom rampConcurrentUsers(int from) {
    return new ClosedInjectionStep.RampConcurrentUsersInjectionFrom(from);
  }

  public static ClosedInjectionStep.IncreasingConcurrentUsersProfileBuilder incrementConcurrentUsers(int concurrentUsers) {
    return new ClosedInjectionStep.IncreasingConcurrentUsersProfileBuilder(concurrentUsers);
  }

  //////////  ThrottlingSupport
  public static ThrottleStep.ReachIntermediate reachRps(int target) {
    return new ThrottleStep.ReachIntermediate(target);
  }

  public static ThrottleStep holdFor(int duration) {
    return holdFor(Duration.ofSeconds(duration));
  }

  public static ThrottleStep holdFor(Duration duration) {
    return new ThrottleStep(new io.gatling.core.controller.throttle.Hold(toScalaDuration(duration)));
  }

  public static ThrottleStep jumpToRps(int target) {
    return new ThrottleStep(new io.gatling.core.controller.throttle.Jump(target));
  }

  //////////  AssertionSupport
  public static Assertion.WithPath global() {
    return new Assertion.WithPath(Global$.MODULE$, Predef$.MODULE$.configuration());
  }

  public static Assertion.WithPath forAll() {
    return new Assertion.WithPath(ForAll$.MODULE$, Predef$.MODULE$.configuration());
  }

  public static Assertion.WithPath details(String... parts) {
    Seq<String> stringSeq = toScalaSeq(parts);
    return new Assertion.WithPath(new Details(stringSeq.toList()), Predef$.MODULE$.configuration());
  }

  ////////// BodySupport
  public static Function<Body, Body.WithBytes> gzipBody() {
    return javaBody -> new Body.WithBytes(BodyProcessors.gzip().apply(javaBody.asScala()));
  }

  public static Function<Body, Body.Default> streamBody() {
    return javaBody -> new Body.Default(BodyProcessors.stream().apply(javaBody.asScala()));
  }

  public static Body.WithString StringBody(String string) {
    return new Body.WithString(Predef$.MODULE$.StringBody(string, Predef$.MODULE$.configuration()));
  }

  public static Body.WithString StringBody(Function<Session, String> f) {
    return new Body.WithString(Predef$.MODULE$.StringBody(toTypedGatlingSessionFunction(f), Predef$.MODULE$.configuration()));
  }

  public static Body.WithBytes RawFileBody(String filePath) {
    return new Body.WithBytes(Predef$.MODULE$.RawFileBody(toStringExpression(filePath), Predef$.MODULE$.rawFileBodies()));
  }

  public static Body.WithBytes RawFileBody(Function<Session, String> filePath) {
    return new Body.WithBytes(Predef$.MODULE$.RawFileBody(toTypedGatlingSessionFunction(filePath), Predef$.MODULE$.rawFileBodies()));
  }

  public static Body.WithString ElFileBody(String filePath) {
    return new Body.WithString(Predef$.MODULE$.ElFileBody(toStringExpression(filePath), Predef$.MODULE$.elFileBodies()));
  }

  public static Body.WithString ElFileBody(Function<Session, String> filePath) {
    return new Body.WithString(Predef$.MODULE$.ElFileBody(toTypedGatlingSessionFunction(filePath), Predef$.MODULE$.elFileBodies()));
  }

  public static Body.WithString PebbleStringBody(String string) {
    return new Body.WithString(Predef$.MODULE$.PebbleStringBody(string, Predef$.MODULE$.configuration()));
  }

  public static Body.WithString PebbleFileBody(String filePath) {
    return new Body.WithString(Predef$.MODULE$.PebbleFileBody(toStringExpression(filePath), Predef$.MODULE$.pebbleFileBodies(), Predef$.MODULE$.configuration()));
  }

  public static Body.WithString PebbleFileBody(Function<Session, String> filePath) {
    return new Body.WithString(Predef$.MODULE$.PebbleFileBody(toTypedGatlingSessionFunction(filePath), Predef$.MODULE$.pebbleFileBodies(), Predef$.MODULE$.configuration()));
  }

  public static Body.WithBytes ByteArrayBody(byte[] bytes) {
    return new Body.WithBytes(Predef$.MODULE$.ByteArrayBody(toStaticValueExpression(bytes)));
  }

  public static Body.WithBytes ByteArrayBody(String bytes) {
    return new Body.WithBytes(Predef$.MODULE$.ByteArrayBody(toBytesExpression(bytes)));
  }

  public static Body.WithBytes ByteArrayBody(Function<Session, byte[]> bytes) {
    return new Body.WithBytes(Predef$.MODULE$.ByteArrayBody(toTypedGatlingSessionFunction(bytes)));
  }

  public static Body InputStreamBody(Function<Session, InputStream> stream) {
    return new Body.Default(Predef$.MODULE$.InputStreamBody(toTypedGatlingSessionFunction(stream)));
  }

  public static void registerPebbleExtensions(Extension... extensions) {
    Predef$.MODULE$.registerPebbleExtensions(toScalaSeq(extensions));
  }
}
