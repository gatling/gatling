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

package io.gatling.javaapi.core;

import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;

import com.mitchellbosecke.pebble.extension.Extension;
import io.gatling.commons.stats.assertion.Details;
import io.gatling.commons.stats.assertion.ForAll$;
import io.gatling.commons.stats.assertion.Global$;
import io.gatling.javaapi.core.condition.*;
import io.gatling.javaapi.core.error.Errors;
import io.gatling.javaapi.core.exec.Execs;
import io.gatling.javaapi.core.feed.Feeds;
import io.gatling.javaapi.core.group.Groups;
import io.gatling.javaapi.core.internal.Converters;
import io.gatling.javaapi.core.internal.CoreCheckBuilders;
import io.gatling.javaapi.core.internal.CoreCheckType;
import io.gatling.javaapi.core.loop.*;
import io.gatling.javaapi.core.pause.Paces;
import io.gatling.javaapi.core.pause.Pauses;
import io.gatling.javaapi.core.pause.RendezVous;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import scala.collection.immutable.Seq;

/** The entrypoint of the Gatling core DSL */
public final class CoreDsl {

  private CoreDsl() {}

  ////////// CoreDsl

  /**
   * Create a new immutable Scenario builder
   *
   * @param name the scenario name
   * @return a new Scenario builder
   */
  @Nonnull
  public static ScenarioBuilder scenario(@Nonnull String name) {
    return new ScenarioBuilder(name);
  }

  /**
   * Create a new AllowList based on some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java regular
   * expression patterns</a>. Typically used to filter HTTP resources.
   *
   * @param patterns some Java regex patterns
   * @return a new AllowList
   */
  @Nonnull
  public static Filter.AllowList AllowList(@Nonnull String... patterns) {
    return AllowList(Arrays.asList(patterns));
  }

  /**
   * Create a new AllowList based on some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java regular
   * expression patterns</a> Typically used to filter HTTP resources.
   *
   * @param patterns some Java regex patterns
   * @return a new AllowList
   */
  @Nonnull
  public static Filter.AllowList AllowList(@Nonnull List<String> patterns) {
    return new Filter.AllowList(patterns);
  }

  /**
   * Create a new DenyList based on some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java regular
   * expression patterns</a> Typically used to filter HTTP resources.
   *
   * @param patterns some Java regex patterns
   * @return a new DenyList
   */
  @Nonnull
  public static Filter.DenyList DenyList(@Nonnull String... patterns) {
    return DenyList(Arrays.asList(patterns));
  }

  /**
   * Create a new DenyList based on some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java regular
   * expression patterns</a> Typically used to filter HTTP resources.
   *
   * @param patterns some Java regex patterns
   * @return a new DenyList
   */
  @Nonnull
  public static Filter.DenyList DenyList(@Nonnull List<String> patterns) {
    return new Filter.DenyList(patterns);
  }

  ////////// StructureBuilder.Execs
  /**
   * Bootstrap a new ChainBuilder from a function that manipulates the {@link Session}, see {@link
   * Execs#exec(Function)}.
   *
   * @param f the function
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder exec(@Nonnull Function<Session, Session> f) {
    return ChainBuilder.EMPTY.exec(f);
  }

  /**
   * Bootstrap a new ChainBuilder from an {@link ActionBuilder}, see {@link
   * Execs#exec(ActionBuilder)}.
   *
   * @param actionBuilder the Action builder
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder exec(@Nonnull ActionBuilder actionBuilder) {
    return ChainBuilder.EMPTY.exec(actionBuilder);
  }

  /**
   * Bootstrap a new {@link ChainBuilder} from some {@link StructureBuilder}s, see {@link
   * Execs#exec(StructureBuilder[])}.
   *
   * @param structureBuilders some {@link ChainBuilder}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static <WB extends io.gatling.core.structure.StructureBuilder<WB>> ChainBuilder exec(
      @Nonnull StructureBuilder<?, WB>... structureBuilders) {
    return exec(Arrays.asList(structureBuilders));
  }

  /**
   * Bootstrap a new {@link ChainBuilder} from some {@link StructureBuilder}s, see {@link
   * Execs#exec(List)}.
   *
   * @param structureBuilders some {@link ChainBuilder}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static <WB extends io.gatling.core.structure.StructureBuilder<WB>> ChainBuilder exec(
      @Nonnull List<StructureBuilder<?, WB>> structureBuilders) {
    return ChainBuilder.EMPTY.exec(structureBuilders);
  }

  ////////// StructureBuilder.Pauses
  /**
   * Bootstrap a new ChainBuilder with a pause, see {@link Pauses#pause(long)}.
   *
   * @param duration the pause duration in seconds
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(long duration) {
    return ChainBuilder.EMPTY.pause(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause, see {@link Pauses#pause(long, PauseType)}.
   *
   * @param duration the pause duration in seconds
   * @param pauseType the type of pause
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(long duration, @Nonnull PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(duration, pauseType);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause, see {@link Pauses#pause(Duration)}.
   *
   * @param duration the pause duration
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(@Nonnull Duration duration) {
    return ChainBuilder.EMPTY.pause(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause, see {@link Pauses#pause(Duration, PauseType)}.
   *
   * @param duration the pause duration
   * @param pauseType the type of pause
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(@Nonnull Duration duration, @Nonnull PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(duration, pauseType);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause as a Gatling Expression Language string, see {@link
   * Pauses#pause(String)}.
   *
   * @param duration the pause duration as a Gatling Expression Language string
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(@Nonnull String duration) {
    return ChainBuilder.EMPTY.pause(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause as a Gatling Expression Language string, see {@link
   * Pauses#pause(String, PauseType)}.
   *
   * @param duration the pause duration as a Gatling Expression Language string
   * @param pauseType the type of pause
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(@Nonnull String duration, @Nonnull PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(duration, pauseType);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause as a function, see {@link Pauses#pause(Function)}.
   *
   * @param f the pause duration as a function
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(@Nonnull Function<Session, Duration> f) {
    return ChainBuilder.EMPTY.pause(f);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause as a function, see {@link Pauses#pause(Function,
   * PauseType)}.
   *
   * @param f the pause duration as a function
   * @param pauseType the type of pause
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(
      @Nonnull Function<Session, Duration> f, @Nonnull PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(f, pauseType);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause computed randomly between 2 values in seconds, see
   * {@link Pauses#pause(long, long)}.
   *
   * @param min the pause minimum in seconds
   * @param max the pause maximum in seconds
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(long min, long max) {
    return ChainBuilder.EMPTY.pause(min, max);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause computed randomly between 2 values in seconds, see
   * {@link Pauses#pause(long, long, PauseType)}.
   *
   * @param min the pause minimum in seconds
   * @param max the pause maximum in seconds
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(long min, long max, @Nonnull PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(min, max, pauseType);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause computed randomly between 2 values, * see {@link
   * Pauses#pause(Duration, Duration)}.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(@Nonnull Duration min, @Nonnull Duration max) {
    return ChainBuilder.EMPTY.pause(min, max);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause computed randomly between 2 values, see {@link
   * Pauses#pause(Duration, Duration, PauseType)}.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @param pauseType the type of pause
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(
      @Nonnull Duration min, @Nonnull Duration max, @Nonnull PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(min, max, pauseType);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause computed randomly between 2 values as a Gatling
   * Expression Language string, see {@link Pauses#pause(String, String)}.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(@Nonnull String min, @Nonnull String max) {
    return ChainBuilder.EMPTY.pause(min, max);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause computed randomly between 2 values as a Gatling
   * Expression Language string, see {@link Pauses#pause(String, String, PauseType)}.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @param pauseType the type of pause
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(
      @Nonnull String min, @Nonnull String max, @Nonnull PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(min, max, pauseType);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause computed randomly between 2 values as functions, see
   * {@link Pauses#pause(Function, Function)}.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(
      @Nonnull Function<Session, Duration> min, @Nonnull Function<Session, Duration> max) {
    return ChainBuilder.EMPTY.pause(min, max);
  }

  /**
   * Bootstrap a new ChainBuilder with a pause computed randomly between 2 values as functions, see
   * {@link Pauses#pause(Function, Function, PauseType)}.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @param pauseType the type of pause
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pause(
      @Nonnull Function<Session, Duration> min,
      @Nonnull Function<Session, Duration> max,
      @Nonnull PauseType pauseType) {
    return ChainBuilder.EMPTY.pause(min, max, pauseType);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action, see {@link Paces#pace(long)}.
   *
   * @param duration the duration of the pace in seconds
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(long duration) {
    return ChainBuilder.EMPTY.pace(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action, see {@link Paces#pace(long, String)}.
   *
   * @param duration the duration of the pace in seconds
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(long duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.pace(duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action, see {@link Paces#pace(Duration)}.
   *
   * @param duration the duration of the pace
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(@Nonnull Duration duration) {
    return ChainBuilder.EMPTY.pace(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action, see {@link Paces#pace(Duration, String)}.
   *
   * @param duration the duration of the pace
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(@Nonnull Duration duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.pace(duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a Gatling Expression Language duration,
   * see {@link Paces#pace(String)}.
   *
   * @param duration the duration of the pace
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(@Nonnull String duration) {
    return ChainBuilder.EMPTY.pace(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a pace action with a Gatling Expression
   * Language duration, see {@link Paces#pace(String, String)}.
   *
   * @param duration the duration of the pace
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(@Nonnull String duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.pace(duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action, see {@link Paces#pace(Function)}.
   *
   * @param duration the duration of the pace
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(@Nonnull Function<Session, Duration> duration) {
    return ChainBuilder.EMPTY.pace(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action, see {@link Paces#pace(Function, String)}.
   *
   * @param duration the duration of the pace
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(
      @Nonnull Function<Session, Duration> duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.pace(duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a random duration between 2 defined
   * limits, see {@link Paces#pace(long, long)}.
   *
   * @param min the minimum duration of the pace in seconds
   * @param max the maximum duration of the pace in seconds
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(long min, long max) {
    return ChainBuilder.EMPTY.pace(min, max);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a random duration between 2 defined
   * limits, see {@link Paces#pace(long, long, String)}.
   *
   * @param min the minimum duration of the pace in seconds
   * @param max the maximum duration of the pace in seconds
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(long min, long max, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.pace(min, max, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a random duration between 2 defined
   * limits, see {@link Paces#pace(Duration, Duration)}.
   *
   * @param min the minimum duration of the pace
   * @param max the maximum duration of the pace
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(@Nonnull Duration min, @Nonnull Duration max) {
    return ChainBuilder.EMPTY.pace(min, max);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a random duration between 2 defined
   * limits, see {@link Paces#pace(Duration, Duration, String)}.
   *
   * @param min the minimum duration of the pace
   * @param max the maximum duration of the pace
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(
      @Nonnull Duration min, @Nonnull Duration max, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.pace(min, max, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a random duration between 2 defined
   * limits, see {@link Paces#pace(String, String, String)}.
   *
   * @param min the minimum duration of the pace as a Gatling Expression Language string
   * @param max the maximum duration of the pace as a Gatling Expression Language string
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(
      @Nonnull String min, @Nonnull String max, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.pace(min, max, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a random duration between 2 defined
   * limits, see {@link Paces#pace(Function, Function)}.
   *
   * @param min the minimum duration of the pace
   * @param max the maximum duration of the pace
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(
      @Nonnull Function<Session, Duration> min, Function<Session, Duration> max) {
    return ChainBuilder.EMPTY.pace(min, max);
  }

  /**
   * Bootstrap a new ChainBuilder with a pace action with a random duration between 2 defined
   * limits, see {@link Paces#pace(Function, Function, String)}.
   *
   * @param min the minimum duration of the pace
   * @param max the maximum duration of the pace
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder pace(
      @Nonnull Function<Session, Duration> min,
      @Nonnull Function<Session, Duration> max,
      @Nonnull String counterName) {
    return ChainBuilder.EMPTY.pace(min, max, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a rendez-vous action, see {@link RendezVous#rendezVous(int)}.
   *
   * @param users the number of users that have to wait
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder rendezVous(int users) {
    return ChainBuilder.EMPTY.rendezVous(users);
  }

  ////////// StructureBuilder.Feeds

  /**
   * Bootstrap a new ChainBuilder with a feed action, see {@link Feeds#feed(Supplier)}.
   *
   * @param feederBuilder a supplier so that the underlying {@link Iterator} can be lazily loaded.
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder feed(@Nonnull Supplier<Iterator<Map<String, Object>>> feederBuilder) {
    return ChainBuilder.EMPTY.feed(feederBuilder);
  }

  /**
   * Bootstrap a new ChainBuilder with a feed action, see {@link Feeds#feed(Iterator)}.
   *
   * @param feeder a source of records.
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder feed(@Nonnull Iterator<Map<String, Object>> feeder) {
    return ChainBuilder.EMPTY.feed(feeder);
  }

  /**
   * Bootstrap a new ChainBuilder with a feed action, see {@link Feeds#feed(FeederBuilder)}.
   *
   * @param feederBuilder a source of records.
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder feed(@Nonnull FeederBuilder<?> feederBuilder) {
    return ChainBuilder.EMPTY.feed(feederBuilder);
  }

  ////////// StructureBuilder.Loops
  /**
   * Bootstrap a new ChainBuilder with a repeat loop, see {@link Repeat#repeat(int)}.
   *
   * @param times the number of iterations
   * @return the next DSL step
   */
  @Nonnull
  public static Repeat.Loop<ChainBuilder> repeat(int times) {
    return ChainBuilder.EMPTY.repeat(times);
  }

  /**
   * Bootstrap a new ChainBuilder with a repeat loop, see {@link Repeat#repeat(int, String)}.
   *
   * @param times the number of iterations
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static Repeat.Loop<ChainBuilder> repeat(int times, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.repeat(times, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a repeat loop, see {@link Repeat#repeat(String)}.
   *
   * @param times the number of iterations as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static Repeat.Loop<ChainBuilder> repeat(@Nonnull String times) {
    return ChainBuilder.EMPTY.repeat(times);
  }

  /**
   * Bootstrap a new ChainBuilder with a repeat loop, see {@link Repeat#repeat(String, String)}.
   *
   * @param times the number of iterations as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static Repeat.Loop<ChainBuilder> repeat(
      @Nonnull String times, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.repeat(times, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a repeat loop, see {@link Repeat#repeat(Function)}.
   *
   * @param times the number of iterations
   * @return the next DSL step
   */
  @Nonnull
  public static Repeat.Loop<ChainBuilder> repeat(@Nonnull Function<Session, Integer> times) {
    return ChainBuilder.EMPTY.repeat(times);
  }

  /**
   * Bootstrap a new ChainBuilder with a repeat loop, see {@link Repeat#repeat(Function, String)}.
   *
   * @param times the number of iterations
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static Repeat.Loop<ChainBuilder> repeat(
      @Nonnull Function<Session, Integer> times, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.repeat(times, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a foreach loop, see {@link ForEach#foreach(List, String)}.
   *
   * @param seq the list to iterate over
   * @param attributeName the key to store the current element
   * @return the next DSL step
   */
  @Nonnull
  public static ForEach.Loop<ChainBuilder> foreach(@Nonnull List<?> seq, String attributeName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName);
  }

  /**
   * Bootstrap a new ChainBuilder with a foreach loop, see {@link ForEach#foreach(List, String,
   * String)}.
   *
   * @param seq the list to iterate over
   * @param attributeName the key to store the current element
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static ForEach.Loop<ChainBuilder> foreach(
      @Nonnull List<?> seq, String attributeName, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a foreach loop, see {@link ForEach#foreach(String, String)}.
   *
   * @param seq the list to iterate over as a Gatling Expression Language String
   * @param attributeName the key to store the current element
   * @return the next DSL step
   */
  @Nonnull
  public static ForEach.Loop<ChainBuilder> foreach(@Nonnull String seq, String attributeName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName);
  }

  /**
   * Bootstrap a new ChainBuilder with a foreach loop, see {@link ForEach#foreach(String, String,
   * String)}.
   *
   * @param seq the list to iterate over as a Gatling Expression Language String
   * @param attributeName the key to store the current element
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static ForEach.Loop<ChainBuilder> foreach(
      @Nonnull String seq, String attributeName, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a foreach loop, see {@link ForEach#foreach(Function,
   * String)}.
   *
   * @param seq the list to iterate over
   * @param attributeName the key to store the current element
   * @return the next DSL step
   */
  @Nonnull
  public static ForEach.Loop<ChainBuilder> foreach(
      @Nonnull Function<Session, List<?>> seq, @Nonnull String attributeName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName);
  }

  /**
   * Bootstrap a new ChainBuilder with a foreach loop, see {@link ForEach#foreach(Function, String,
   * String)}.
   *
   * @param seq the list to iterate over
   * @param attributeName the key to store the current element
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static ForEach.Loop<ChainBuilder> foreach(
      @Nonnull Function<Session, List<?>> seq,
      @Nonnull String attributeName,
      @Nonnull String counterName) {
    return ChainBuilder.EMPTY.foreach(seq, attributeName, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(long)}.
   *
   * @param duration the loop duration in seconds
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(long duration) {
    return ChainBuilder.EMPTY.during(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(long, boolean)}.
   *
   * @param duration the loop duration in seconds
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(long duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(long, String)}.
   *
   * @param duration the loop duration in seconds
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(long duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.during(duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(long, String,
   * boolean)}.
   *
   * @param duration the loop duration in seconds
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(
      long duration, @Nonnull String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(Duration)}.
   *
   * @param duration the loop duration
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(@Nonnull Duration duration) {
    return ChainBuilder.EMPTY.during(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(Duration, boolean)}.
   *
   * @param duration the loop duration
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(@Nonnull Duration duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(Duration, String)}.
   *
   * @param duration the loop duration
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(
      @Nonnull Duration duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.during(duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(Duration, String,
   * boolean)}.
   *
   * @param duration the loop duration
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(
      @Nonnull Duration duration, @Nonnull String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(String)}.
   *
   * @param duration the loop duration as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(@Nonnull String duration) {
    return ChainBuilder.EMPTY.during(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(String, boolean)}.
   *
   * @param duration the loop duration as a Gatling Expression Language String
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(@Nonnull String duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(String, String)}.
   *
   * @param duration the loop duration as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(
      @Nonnull String duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.during(duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(String, String,
   * boolean)}.
   *
   * @param duration the loop duration as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(
      @Nonnull String duration, @Nonnull String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(Function)}.
   *
   * @param duration the loop duration as a function
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(@Nonnull Function<Session, Duration> duration) {
    return ChainBuilder.EMPTY.during(duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(Function, boolean)}.
   *
   * @param duration the loop duration as a function
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(
      @Nonnull Function<Session, Duration> duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(Function, String)}.
   *
   * @param duration the loop duration as a function
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(
      @Nonnull Function<Session, Duration> duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.during(duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a during loop, see {@link During#during(Function, String,
   * boolean)}.
   *
   * @param duration the loop duration as a function
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static During.Loop<ChainBuilder> during(
      @Nonnull Function<Session, Duration> duration,
      @Nonnull String counterName,
      boolean exitASAP) {
    return ChainBuilder.EMPTY.during(duration, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a forever loop, see {@link Forever#forever()}.
   *
   * @return the next DSL step
   */
  @Nonnull
  public static Forever.Loop<ChainBuilder> forever() {
    return ChainBuilder.EMPTY.forever();
  }

  /**
   * Bootstrap a new ChainBuilder with a forever loop, see {@link Forever#forever(String)}.
   *
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static Forever.Loop<ChainBuilder> forever(@Nonnull String counterName) {
    return ChainBuilder.EMPTY.forever(counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAs loop, see {@link AsLongAs#asLongAs(String)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAs.Loop<ChainBuilder> asLongAs(@Nonnull String condition) {
    return ChainBuilder.EMPTY.asLongAs(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAs loop, see {@link AsLongAs#asLongAs(String,
   * String)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAs.Loop<ChainBuilder> asLongAs(
      @Nonnull String condition, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.asLongAs(condition, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAs loop, see {@link AsLongAs#asLongAs(String,
   * boolean)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAs.Loop<ChainBuilder> asLongAs(@Nonnull String condition, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAs(condition, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAs loop, see {@link AsLongAs#asLongAs(String, String,
   * boolean)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAs.Loop<ChainBuilder> asLongAs(
      @Nonnull String condition, @Nonnull String counterName, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAs(condition, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAs loop, see {@link AsLongAs#asLongAs(Function)}.
   *
   * @param condition the loop condition as a function
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAs.Loop<ChainBuilder> asLongAs(
      @Nonnull Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.asLongAs(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAs loop, see {@link AsLongAs#asLongAs(Function,
   * String)}.
   *
   * @param condition the loop condition as a function
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAs.Loop<ChainBuilder> asLongAs(
      @Nonnull Function<Session, Boolean> condition, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.asLongAs(condition, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAs loop, see {@link AsLongAs#asLongAs(Function,
   * boolean)}.
   *
   * @param condition the loop condition as a function
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAs.Loop<ChainBuilder> asLongAs(
      @Nonnull Function<Session, Boolean> condition, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAs(condition, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAs loop, see {@link AsLongAs#asLongAs(Function,
   * String, boolean)}.
   *
   * @param condition the loop condition as a function
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAs.Loop<ChainBuilder> asLongAs(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull String counterName,
      boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAs(condition, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhile loop, see {@link DoWhile#doWhile(String)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhile.Loop<ChainBuilder> doWhile(@Nonnull String condition) {
    return ChainBuilder.EMPTY.doWhile(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhile loop, see {@link DoWhile#doWhile(String, String)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhile.Loop<ChainBuilder> doWhile(
      @Nonnull String condition, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.doWhile(condition, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhile loop, see {@link DoWhile#doWhile(Function)}.
   *
   * @param condition the loop condition as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhile.Loop<ChainBuilder> doWhile(@Nonnull Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.doWhile(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhile loop, see {@link DoWhile#doWhile(Function,
   * String)}.
   *
   * @param condition the loop condition as a function
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhile.Loop<ChainBuilder> doWhile(
      @Nonnull Function<Session, Boolean> condition, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.doWhile(condition, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAsDuring loop, see {@link
   * AsLongAsDuring#asLongAsDuring(String, String)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param duration the loop max duration as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(
      @Nonnull String condition, @Nonnull String duration) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAsDuring loop, see {@link
   * AsLongAsDuring#asLongAsDuring(String, String, String)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param duration the loop max duration as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(
      @Nonnull String condition, @Nonnull String duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAsDuring loop, see {@link
   * AsLongAsDuring#asLongAsDuring(String, String, boolean)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param duration the loop max duration as a Gatling Expression Language String
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(
      @Nonnull String condition, @Nonnull String duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAsDuring loop, see {@link
   * AsLongAsDuring#asLongAsDuring(String, String, String, boolean)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param duration the loop max duration as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(
      @Nonnull String condition,
      @Nonnull String duration,
      @Nonnull String counterName,
      boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAsDuring loop, see {@link
   * AsLongAsDuring#asLongAsDuring(Function, Function)}.
   *
   * @param condition the loop condition as a function
   * @param duration the loop max duration as a function
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAsDuring loop, see {@link
   * AsLongAsDuring#asLongAsDuring(Function, Function, String)}.
   *
   * @param condition the loop condition as a function
   * @param duration the loop max duration as a function
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      @Nonnull String counterName) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAsDuring loop, see {@link
   * AsLongAsDuring#asLongAsDuring(Function, Function, boolean)}.
   *
   * @param condition the loop condition as a function
   * @param duration the loop max duration as a function
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a asLongAsDuring loop, see {@link
   * AsLongAsDuring#asLongAsDuring(Function, Function, String, boolean)}.
   *
   * @param condition the loop condition as a function
   * @param duration the loop max duration as a function
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static AsLongAsDuring.Loop<ChainBuilder> asLongAsDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      @Nonnull String counterName,
      boolean exitASAP) {
    return ChainBuilder.EMPTY.asLongAsDuring(condition, duration, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhileDuring loop, see {@link
   * DoWhileDuring#doWhileDuring(Function, Function)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param duration the loop max duration as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(
      @Nonnull String condition, @Nonnull String duration) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhileDuring loop, see {@link
   * DoWhileDuring#doWhileDuring(Function, Function, String)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param duration the loop max duration as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(
      @Nonnull String condition, @Nonnull String duration, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhileDuring loop, see {@link
   * DoWhileDuring#doWhileDuring(Function, Function, boolean)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param duration the loop max duration as a Gatling Expression Language String
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(
      @Nonnull String condition, @Nonnull String duration, boolean exitASAP) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhileDuring loop, see {@link
   * DoWhileDuring#doWhileDuring(Function, Function, String, boolean)}.
   *
   * @param condition the loop condition as a Gatling Expression Language String
   * @param duration the loop max duration as a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(
      @Nonnull String condition,
      @Nonnull String duration,
      @Nonnull String counterName,
      boolean exitASAP) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, counterName, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhileDuring loop, see {@link
   * DoWhileDuring#doWhileDuring(Function, Function)}.
   *
   * @param condition the loop condition as a function
   * @param duration the loop max duration as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhileDuring loop, see {@link
   * DoWhileDuring#doWhileDuring(Function, Function, String)}.
   *
   * @param condition the loop condition as a function
   * @param duration the loop max duration as a function
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      @Nonnull String counterName) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhileDuring loop, see {@link
   * DoWhileDuring#doWhileDuring(Function, Function, boolean)}.
   *
   * @param condition the loop condition as a function
   * @param duration the loop max duration as a function
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      boolean exitASAP) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, exitASAP);
  }

  /**
   * Bootstrap a new ChainBuilder with a doWhileDuring loop, see {@link
   * DoWhileDuring#doWhileDuring(Function, Function, String, boolean)}.
   *
   * @param condition the loop condition as a function
   * @param duration the loop max duration as a function
   * @param counterName the name of the loop counter
   * @param exitASAP if the loop must be interrupted
   * @return the next DSL step
   */
  @Nonnull
  public static DoWhileDuring.Loop<ChainBuilder> doWhileDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      String counterName,
      boolean exitASAP) {
    return ChainBuilder.EMPTY.doWhileDuring(condition, duration, counterName, exitASAP);
  }

  ////////// StructureBuilder.ConditionalStatements
  /**
   * Bootstrap a new ChainBuilder with a doIf block, see {@link DoIf#doIf(String)}.
   *
   * @param condition the condition as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoIf.Then<ChainBuilder> doIf(@Nonnull String condition) {
    return ChainBuilder.EMPTY.doIf(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIf block, see {@link DoIf#doIf(Function)}.
   *
   * @param condition the condition as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoIf.Then<ChainBuilder> doIf(@Nonnull Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.doIf(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfOrElse block, see {@link
   * DoIfOrElse#doIfOrElse(String)}.
   *
   * @param condition the condition as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfOrElse.Then<ChainBuilder> doIfOrElse(@Nonnull String condition) {
    return ChainBuilder.EMPTY.doIfOrElse(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfOrElse block, see {@link
   * DoIfOrElse#doIfOrElse(Function)}.
   *
   * @param condition the condition as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfOrElse.Then<ChainBuilder> doIfOrElse(
      @Nonnull Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.doIfOrElse(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEquals block, see {@link DoIfEquals#doIfEquals(String,
   * String)}.
   *
   * @param actual the actual value as a Gatling Expression Language String
   * @param expected the expected value as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEquals.Then<ChainBuilder> doIfEquals(
      @Nonnull String actual, @Nonnull String expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEquals block, see {@link DoIfEquals#doIfEquals(String,
   * Object)}.
   *
   * @param actual the actual value as a Gatling Expression Language String
   * @param expected the expected value as a static value
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEquals.Then<ChainBuilder> doIfEquals(
      @Nonnull String actual, @Nonnull Object expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEquals block, see {@link DoIfEquals#doIfEquals(String,
   * Function)}.
   *
   * @param actual the actual value as a Gatling Expression Language String
   * @param expected the expected value as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEquals.Then<ChainBuilder> doIfEquals(
      @Nonnull String actual, @Nonnull Function<Session, Object> expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEquals block, see {@link
   * DoIfEquals#doIfEquals(Function, String)}.
   *
   * @param actual the actual value as a function
   * @param expected the expected value as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEquals.Then<ChainBuilder> doIfEquals(
      @Nonnull Function<Session, Object> actual, @Nonnull String expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEquals block, see {@link
   * DoIfEquals#doIfEquals(Function, Object)}.
   *
   * @param actual the actual value as a function
   * @param expected the expected value as a static value
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEquals.Then<ChainBuilder> doIfEquals(
      @Nonnull Function<Session, Object> actual, Object expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEquals block, see {@link
   * DoIfEquals#doIfEquals(Function, Function)}.
   *
   * @param actual the actual value as a function
   * @param expected the expected value as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEquals.Then<ChainBuilder> doIfEquals(
      @Nonnull Function<Session, Object> actual, @Nonnull Function<Session, Object> expected) {
    return ChainBuilder.EMPTY.doIfEquals(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEqualsOrElse block, see {@link
   * DoIfEqualsOrElse#doIfEqualsOrElse(String, String)}.
   *
   * @param actual the actual value as a Gatling Expression Language String
   * @param expected the expected value as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(
      String actual, String expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEqualsOrElse block, see {@link
   * DoIfEqualsOrElse#doIfEqualsOrElse(String, Object)}.
   *
   * @param actual the actual value as a Gatling Expression Language String
   * @param expected the expected value as a static value
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(
      @Nonnull String actual, @Nonnull Object expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEqualsOrElse block, see {@link
   * DoIfEqualsOrElse#doIfEqualsOrElse(String, Function)}.
   *
   * @param actual the actual value as a Gatling Expression Language String
   * @param expected the expected value as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(
      @Nonnull String actual, @Nonnull Function<Session, Object> expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEqualsOrElse block, see {@link
   * DoIfEqualsOrElse#doIfEqualsOrElse(Function, String)}.
   *
   * @param actual the actual value as a function
   * @param expected the expected value as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(
      @Nonnull Function<Session, Object> actual, @Nonnull String expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEqualsOrElse block, see {@link
   * DoIfEqualsOrElse#doIfEqualsOrElse(Function, Object)}.
   *
   * @param actual the actual value as a function
   * @param expected the expected value as a static value
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(
      @Nonnull Function<Session, Object> actual, @Nonnull Object expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doIfEqualsOrElse block, see {@link
   * DoIfEqualsOrElse#doIfEqualsOrElse(Function, Function)}.
   *
   * @param actual the actual value as a function
   * @param expected the expected value as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoIfEqualsOrElse.Then<ChainBuilder> doIfEqualsOrElse(
      @Nonnull Function<Session, Object> actual, @Nonnull Function<Session, Object> expected) {
    return ChainBuilder.EMPTY.doIfEqualsOrElse(actual, expected);
  }

  /**
   * Bootstrap a new ChainBuilder with a doSwitch block, see {@link DoSwitch#doSwitch(String)}.
   *
   * @param actual the actual value as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoSwitch.Choices<ChainBuilder> doSwitch(@Nonnull String actual) {
    return ChainBuilder.EMPTY.doSwitch(actual);
  }

  /**
   * Bootstrap a new ChainBuilder with a doSwitch block, see {@link DoSwitch#doSwitch(Function)}.
   *
   * @param actual the actual value as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoSwitch.Choices<ChainBuilder> doSwitch(@Nonnull Function<Session, Object> actual) {
    return ChainBuilder.EMPTY.doSwitch(actual);
  }

  /**
   * Bootstrap a new ChainBuilder with a doSwitchOrElse block, see {@link
   * DoSwitchOrElse#doSwitchOrElse(String)}.
   *
   * @param actual the actual value as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static DoSwitchOrElse.Choices<ChainBuilder> doSwitchOrElse(@Nonnull String actual) {
    return ChainBuilder.EMPTY.doSwitchOrElse(actual);
  }

  /**
   * Bootstrap a new ChainBuilder with a doSwitchOrElse block, see {@link
   * DoSwitchOrElse#doSwitchOrElse(Function)}.
   *
   * @param actual the actual value as a function
   * @return the next DSL step
   */
  @Nonnull
  public static DoSwitchOrElse.Choices<ChainBuilder> doSwitchOrElse(
      @Nonnull Function<Session, Object> actual) {
    return ChainBuilder.EMPTY.doSwitchOrElse(actual);
  }

  /**
   * Bootstrap a new ChainBuilder with a randomSwitch block.
   *
   * @return the next DSL step
   */
  @Nonnull
  public static RandomSwitch.Choices<ChainBuilder> randomSwitch() {
    return ChainBuilder.EMPTY.randomSwitch();
  }

  /**
   * Bootstrap a new ChainBuilder with a randomSwitchOrElse block, see {@link
   * RandomSwitchOrElse#randomSwitchOrElse}.
   *
   * @return the next DSL step
   */
  @Nonnull
  public static RandomSwitchOrElse.Choices<ChainBuilder> randomSwitchOrElse() {
    return ChainBuilder.EMPTY.randomSwitchOrElse();
  }

  /**
   * Bootstrap a new ChainBuilder with a uniformRandomSwitch block, see {@link
   * UniformRandomSwitch#uniformRandomSwitch)}.
   *
   * @return the next DSL step
   */
  @Nonnull
  public static UniformRandomSwitch.Choices<ChainBuilder> uniformRandomSwitch() {
    return ChainBuilder.EMPTY.uniformRandomSwitch();
  }

  /**
   * Bootstrap a new ChainBuilder with a roundRobinSwitch block, see {@link
   * RoundRobinSwitch#roundRobinSwitch}.
   *
   * @return the next DSL step
   */
  @Nonnull
  public static RoundRobinSwitch.Choices<ChainBuilder> roundRobinSwitch() {
    return ChainBuilder.EMPTY.roundRobinSwitch();
  }

  ////////// StructureBuilder.Errors
  /**
   * Bootstrap a new ChainBuilder with a exitBlockOnFail block, see {@link
   * Errors#exitBlockOnFail(ChainBuilder)}.
   *
   * @param chain the chain to interrupt on error
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder exitBlockOnFail(@Nonnull ChainBuilder chain) {
    return ChainBuilder.EMPTY.exitBlockOnFail(chain);
  }

  /**
   * Bootstrap a new ChainBuilder with a tryMax block, see {@link Errors#tryMax(int)}.
   *
   * @param times the maximum number of times to try to execute the chain successfully, expressed as
   *     a static value
   * @return the next DSL step
   */
  @Nonnull
  public static Errors.TryMax<ChainBuilder> tryMax(int times) {
    return ChainBuilder.EMPTY.tryMax(times);
  }

  /**
   * Bootstrap a new ChainBuilder with a tryMax block, see {@link Errors#tryMax(int, String)}.
   *
   * @param times the maximum number of times to try to execute the chain successfully, expressed as
   *     a static value
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static Errors.TryMax<ChainBuilder> tryMax(int times, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.tryMax(times, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a tryMax block, see {@link Errors#tryMax(String)}.
   *
   * @param times the maximum number of times to try to execute the chain successfully, expressed as
   *     a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static Errors.TryMax<ChainBuilder> tryMax(@Nonnull String times) {
    return ChainBuilder.EMPTY.tryMax(times);
  }

  /**
   * Bootstrap a new ChainBuilder with a tryMax block, see {@link Errors#tryMax(String, String)}.
   *
   * @param times the maximum number of times to try to execute the chain successfully, expressed as
   *     a Gatling Expression Language String
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static Errors.TryMax<ChainBuilder> tryMax(
      @Nonnull String times, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.tryMax(times, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a tryMax block, see {@link Errors#tryMax(Function)}.
   *
   * @param times the maximum number of times to try to execute the chain successfully, expressed as
   *     a function
   * @return the next DSL step
   */
  @Nonnull
  public static Errors.TryMax<ChainBuilder> tryMax(@Nonnull Function<Session, Integer> times) {
    return ChainBuilder.EMPTY.tryMax(times);
  }

  /**
   * Bootstrap a new ChainBuilder with a tryMax block, see {@link Errors#tryMax(Function, String)}.
   *
   * @param times the maximum number of times to try to execute the chain successfully, expressed as
   *     a function
   * @param counterName the name of the loop counter
   * @return the next DSL step
   */
  @Nonnull
  public static Errors.TryMax<ChainBuilder> tryMax(
      @Nonnull Function<Session, Integer> times, @Nonnull String counterName) {
    return ChainBuilder.EMPTY.tryMax(times, counterName);
  }

  /**
   * Bootstrap a new ChainBuilder with a exitHereIf block, see {@link Errors#exitHereIf(String)}.
   *
   * @param condition the condition to trigger the exit, expressed as Gatling Expression Language
   *     String
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder exitHereIf(@Nonnull String condition) {
    return ChainBuilder.EMPTY.exitHereIf(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a exitBlockOnFail block, see {@link
   * Errors#exitHereIf(Function)}.
   *
   * @param condition the condition to trigger the exit, expressed as Function
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder exitHereIf(@Nonnull Function<Session, Boolean> condition) {
    return ChainBuilder.EMPTY.exitHereIf(condition);
  }

  /**
   * Bootstrap a new ChainBuilder with a exitHere block, see {@link Errors#exitHere()}.
   *
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder exitHere() {
    return ChainBuilder.EMPTY.exitHere();
  }

  /**
   * Bootstrap a new ChainBuilder with a exitHereIfFailed block, see {@link
   * Errors#exitHereIfFailed()}.
   *
   * @return a new ChainBuilder
   */
  @Nonnull
  public static ChainBuilder exitHereIfFailed() {
    return ChainBuilder.EMPTY.exitHereIfFailed();
  }

  ////////// StructureBuilder.Groups
  /**
   * Bootstrap a new ChainBuilder with a group block, see {@link Groups#group(String)}.
   *
   * @param name the name of the group, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static Groups.Grouping<ChainBuilder> group(@Nonnull String name) {
    return ChainBuilder.EMPTY.group(name);
  }

  /**
   * Bootstrap a new ChainBuilder with a group block, see {@link Groups#group(Function)}.
   *
   * @param name the name of the group, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static Groups.Grouping<ChainBuilder> group(@Nonnull Function<Session, String> name) {
    return ChainBuilder.EMPTY.group(name);
  }

  ////////// PauseSupport
  /** A shortcut for {@link PauseType#Disabled} */
  public static PauseType disabledPauses = PauseType.Disabled;

  /** A shortcut for {@link PauseType#Constant} */
  public static PauseType constantPauses = PauseType.Constant;

  /** A shortcut for {@link PauseType#Exponential} */
  public static PauseType exponentialPauses = PauseType.Exponential;

  /** A shortcut for {@link PauseType.NormalWithPercentageDuration(double)} */
  @Nonnull
  public static PauseType normalPausesWithPercentageDuration(double stdDev) {
    return new PauseType.NormalWithPercentageDuration(stdDev);
  }

  /** A shortcut for {@link PauseType.NormalWithStdDevDuration(Duration)} */
  @Nonnull
  public static PauseType normalPausesWithStdDevDuration(@Nonnull Duration stdDev) {
    return new PauseType.NormalWithStdDevDuration(stdDev);
  }

  /** A shortcut for {@link PauseType.Custom(Function)} */
  @Nonnull
  public static PauseType customPauses(@Nonnull Function<Session, Long> f) {
    return new PauseType.Custom(f);
  }

  /** A shortcut for {@link PauseType.UniformPercentage(double)} */
  @Nonnull
  public static PauseType uniformPausesPlusOrMinusPercentage(double plusOrMinus) {
    return new PauseType.UniformPercentage(plusOrMinus);
  }

  /** A shortcut for {@link PauseType.UniformDuration(Duration)} */
  @Nonnull
  public static PauseType uniformPausesPlusOrMinusDuration(@Nonnull Duration plusOrMinus) {
    return new PauseType.UniformDuration(plusOrMinus);
  }

  ////////// CheckSupport
  // Couldn't manage to implement free checkIf
  /**
   * Bootstrap a new bodyString check that extracts the full response message body as a String.
   * Encoding is either the one provided in the message (eg Content-Type charset attribute in HTTP),
   * or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.Find<String> bodyString() {
    return new CheckBuilder.Find.Default<>(
        io.gatling.core.Predef.bodyString(), CoreCheckType.BodyString, Function.identity());
  }

  /**
   * Bootstrap a new bodyBytes check that extracts the full response message body as a byte array.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.Find<byte[]> bodyBytes() {
    return new CheckBuilder.Find.Default<>(
        io.gatling.core.Predef.bodyBytes(), CoreCheckType.BodyBytes, Function.identity());
  }

  /**
   * Bootstrap a new bodyLength check that extracts the full response message body's binary length.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.Find<Integer> bodyLength() {
    return CoreCheckBuilders.bodyLength();
  }

  /**
   * Bootstrap a new bodyStream check that extracts the full response message body as an {@link
   * InputStream}.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.Find<InputStream> bodyStream() {
    return new CheckBuilder.Find.Default<>(
        io.gatling.core.Predef.bodyStream(), CoreCheckType.BodyStream, Function.identity());
  }

  /**
   * Bootstrap a new substring check that extracts the indexes of the occurrences of a pattern in
   * the response's body String. Encoding is either the one provided in the message (eg Content-Type
   * charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param pattern the searched pattern, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<Integer> substring(@Nonnull String pattern) {
    return CoreCheckBuilders.substring(pattern);
  }

  /**
   * Bootstrap a new substring check that extracts the indexes of the occurrences of a pattern in
   * the response's body String. Encoding is either the one provided in the message (eg Content-Type
   * charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param pattern the searched pattern, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<Integer> substring(
      @Nonnull Function<Session, String> pattern) {
    return CoreCheckBuilders.substring(pattern);
  }

  /**
   * Bootstrap a new xpath check that extracts nodes with a <a
   * href="https://en.wikipedia.org/wiki/XPath">XPath</a> from response's body <a
   * href="https://en.wikipedia.org/wiki/XML">XML</a> document. Encoding is either the one provided
   * in the message (eg Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<String> xpath(@Nonnull String path) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.xpath(
            toStringExpression(path), io.gatling.core.Predef.defaultXmlParsers()),
        CoreCheckType.XPath,
        Function.identity());
  }

  /**
   * Bootstrap a new xpath check that extracts nodes with a <a
   * href="https://en.wikipedia.org/wiki/XPath">XPath</a> from response's body <a
   * href="https://en.wikipedia.org/wiki/XML">XML</a> document. Encoding is either the one provided
   * in the message (eg Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<String> xpath(@Nonnull Function<Session, String> path) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.xpath(
            javaFunctionToExpression(path), io.gatling.core.Predef.defaultXmlParsers()),
        CoreCheckType.XPath,
        Function.identity());
  }

  /**
   * Bootstrap a new xpath check that extracts nodes with a <a
   * href="https://en.wikipedia.org/wiki/XPath">XPath</a> from response's body <a
   * href="https://en.wikipedia.org/wiki/XML">XML</a> document. Encoding is either the one provided
   * in the message (eg Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a Gatling Expression Language String
   * @param namespaces the XML <a href="https://en.wikipedia.org/wiki/XML_namespace">namespaces</a>
   *     used in the document
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<String> xpath(
      @Nonnull String path, @Nonnull Map<String, String> namespaces) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.xpath(
            toStringExpression(path),
            toScalaMap(namespaces),
            io.gatling.core.Predef.defaultXmlParsers()),
        CoreCheckType.XPath,
        Function.identity());
  }

  /**
   * Bootstrap a new xpath check that extracts nodes with a <a
   * href="https://en.wikipedia.org/wiki/XPath">XPath</a> from response's body <a
   * href="https://en.wikipedia.org/wiki/XML">XML</a> document. Encoding is either the one provided
   * in the message (eg Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a function
   * @param namespaces the XML <a href="https://en.wikipedia.org/wiki/XML_namespace">namespaces</a>
   *     used in the document
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<String> xpath(
      @Nonnull Function<Session, String> path, @Nonnull Map<String, String> namespaces) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.xpath(
            javaFunctionToExpression(path),
            toScalaMap(namespaces),
            io.gatling.core.Predef.defaultXmlParsers()),
        CoreCheckType.XPath,
        Function.identity());
  }

  /**
   * Bootstrap a new css check that extracts nodes with a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors">CSS Selector</a> from
   * response's body HTML document. Encoding is either the one provided in the message (eg
   * Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param selector the searched selector, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<String> css(@Nonnull String selector) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.css(
            toStringExpression(selector), io.gatling.core.Predef.defaultCssSelectors()),
        CoreCheckType.Css,
        Function.identity());
  }

  /**
   * Bootstrap a new css check that extracts nodes with a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors">CSS Selector</a> from
   * response's body HTML document. Encoding is either the one provided in the message (eg
   * Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param selector the searched selector, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<String> css(@Nonnull Function<Session, String> selector) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.css(
            javaFunctionToExpression(selector), io.gatling.core.Predef.defaultCssSelectors()),
        CoreCheckType.Css,
        Function.identity());
  }

  /**
   * Bootstrap a new css check that extracts nodes with a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors">CSS Selector</a> from
   * response's body HTML document. Encoding is either the one provided in the message (eg
   * Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param selector the searched selector, expressed as a Gatling Expression Language String
   * @param nodeAttribute the attribute of the selected nodes to capture, if not the node itself
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<String> css(
      @Nonnull String selector, @Nonnull String nodeAttribute) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.css(
            toStringExpression(selector),
            nodeAttribute,
            io.gatling.core.Predef.defaultCssSelectors()),
        CoreCheckType.Css,
        Function.identity());
  }

  /**
   * Bootstrap a new css check that extracts nodes with a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors">CSS Selector</a> from
   * response's body HTML document. Encoding is either the one provided in the message (eg
   * Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param selector the searched selector, expressed as a function
   * @param nodeAttribute the attribute of the selected nodes to capture, if not the node itself
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<String> css(
      @Nonnull Function<Session, String> selector, @Nonnull String nodeAttribute) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.css(
            javaFunctionToExpression(selector),
            nodeAttribute,
            io.gatling.core.Predef.defaultCssSelectors()),
        CoreCheckType.Css,
        Function.identity());
  }

  /**
   * Bootstrap a new form check that extracts an HTML form's input nodes with a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors">CSS Selector</a> from
   * response's body HTML document. Encoding is either the one provided in the message (eg
   * Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param selector the searched selector, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<Map<String, Object>> form(@Nonnull String selector) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.form(
            toStringExpression(selector), io.gatling.core.Predef.defaultCssSelectors()),
        CoreCheckType.Css,
        Converters::toJavaMap);
  }

  /**
   * Bootstrap a new form check that extracts an HTML form's input nodes with a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors">CSS Selector</a> from
   * response's body HTML document. Encoding is either the one provided in the message (eg
   * Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param selector the searched selector, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.MultipleFind<Map<String, Object>> form(
      @Nonnull Function<Session, String> selector) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.core.Predef.form(
            javaFunctionToExpression(selector), io.gatling.core.Predef.defaultCssSelectors()),
        CoreCheckType.Css,
        Converters::toJavaMap);
  }

  /**
   * Bootstrap a new jsonPath check that extracts nodes with a <a
   * href="https://goessner.net/articles/JsonPath/">JsonPath</a> path from response's body JSON
   * tree.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.JsonOfTypeMultipleFind jsonPath(@Nonnull String path) {
    return new CheckBuilder.JsonPath(
        io.gatling.core.Predef.jsonPath(
            toStringExpression(path), io.gatling.core.Predef.defaultJsonPaths()));
  }

  /**
   * Bootstrap a new jsonPath check that extracts nodes with a <a
   * href="https://goessner.net/articles/JsonPath/">JsonPath</a> path from response's body JSON
   * tree.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.JsonOfTypeMultipleFind jsonPath(
      @Nonnull Function<Session, String> path) {
    return new CheckBuilder.JsonPath(
        io.gatling.core.Predef.jsonPath(
            javaFunctionToExpression(path), io.gatling.core.Predef.defaultJsonPaths()));
  }

  /**
   * Bootstrap a new jmesPath check that extracts nodes with a <a
   * href="https://jmespath.org/">JMESPath</a> path from response's body JSON tree.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.JsonOfTypeFind jmesPath(@Nonnull String path) {
    return new CheckBuilder.JmesPath(
        io.gatling.core.Predef.jmesPath(
            toStringExpression(path), io.gatling.core.Predef.defaultJmesPaths()));
  }

  /**
   * Bootstrap a new jmesPath check that extracts nodes with a <a
   * href="https://jmespath.org/">JMESPath</a> path from response's body JSON tree.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.JsonOfTypeFind jmesPath(@Nonnull Function<Session, String> path) {
    return new CheckBuilder.JmesPath(
        io.gatling.core.Predef.jmesPath(
            javaFunctionToExpression(path), io.gatling.core.Predef.defaultJmesPaths()));
  }

  /**
   * Bootstrap a new jsonpJsonPath check that extracts nodes with a <a
   * href="https://goessner.net/articles/JsonPath/">JsonPath</a> path from response's body <a
   * href="https://en.wikipedia.org/wiki/JSONP">JSONP</a> payload.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.JsonOfTypeMultipleFind jsonpJsonPath(@Nonnull String path) {
    return new CheckBuilder.JsonpJsonPath(
        io.gatling.core.Predef.jsonpJsonPath(
            toStringExpression(path), io.gatling.core.Predef.defaultJsonPaths()));
  }

  /**
   * Bootstrap a new jsonpJsonPath check that extracts nodes with a <a
   * href="https://goessner.net/articles/JsonPath/">JsonPath</a> path from response's body <a
   * href="https://en.wikipedia.org/wiki/JSONP">JSONP</a> payload.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.JsonOfTypeMultipleFind jsonpJsonPath(
      @Nonnull Function<Session, String> path) {
    return new CheckBuilder.JsonpJsonPath(
        io.gatling.core.Predef.jsonpJsonPath(
            javaFunctionToExpression(path), io.gatling.core.Predef.defaultJsonPaths()));
  }

  /**
   * Bootstrap a new jsonpJmesPath check that extracts nodes with a <a
   * href="https://jmespath.org/">JMESPath</a> path from response's body JSON <a
   * href="https://en.wikipedia.org/wiki/JSONP">JSONP</a> payload.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.JsonOfTypeFind jsonpJmesPath(@Nonnull String path) {
    return new CheckBuilder.JsonpJmesPath(
        io.gatling.core.Predef.jsonpJmesPath(
            toStringExpression(path), io.gatling.core.Predef.defaultJmesPaths()));
  }

  /**
   * Bootstrap a new jsonpJmesPath check that extracts nodes with a <a
   * href="https://jmespath.org/">JMESPath</a> path from response's body JSON <a
   * href="https://en.wikipedia.org/wiki/JSONP">JSONP</a> payload.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param path the searched path, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.JsonOfTypeFind jsonpJmesPath(@Nonnull Function<Session, String> path) {
    return new CheckBuilder.JsonpJmesPath(
        io.gatling.core.Predef.jsonpJmesPath(
            javaFunctionToExpression(path), io.gatling.core.Predef.defaultJmesPaths()));
  }

  /**
   * Bootstrap a new regex check that extracts capture groups with a <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> from response's body String. Encoding is either the one provided in the message
   * (eg Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param pattern the searched pattern, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.CaptureGroupCheckBuilder regex(@Nonnull String pattern) {
    return new CheckBuilder.Regex(
        io.gatling.core.Predef.regex(
            toStringExpression(pattern), io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Bootstrap a new regex check that extracts capture groups with a <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> from response's body String. Encoding is either the one provided in the message
   * (eg Content-Type charset attribute in HTTP), or the one defined in gatling.conf.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @param pattern the searched pattern, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.CaptureGroupCheckBuilder regex(
      @Nonnull Function<Session, String> pattern) {
    return new CheckBuilder.Regex(
        io.gatling.core.Predef.regex(
            javaFunctionToExpression(pattern), io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Register some <a href="https://github.com/burtcorp/jmespath-java#extensions">custom JMESPath
   * functions</a>
   *
   * @param functions the custom functions to register
   */
  public static void registerJmesPathFunctions(
      @Nonnull io.burt.jmespath.function.Function... functions) {
    io.gatling.core.Predef.registerJmesPathFunctions(toScalaSeq(functions));
  }

  /**
   * Bootstrap a new md5 check that extracts the <a href="https://en.wikipedia.org/wiki/MD5">MD5</a>
   * checksum of the response's body.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.Find<String> md5() {
    return new CheckBuilder.Find.Default<>(
        io.gatling.core.Predef.md5(), CoreCheckType.Md5, Function.identity());
  }

  /**
   * Bootstrap a new sha1 check that extracts the <a
   * href="https://en.wikipedia.org/wiki/SHA-1">SHA-1</a> checksum of the response's body.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.Find<String> sha1() {
    return new CheckBuilder.Find.Default<>(
        io.gatling.core.Predef.sha1(), CoreCheckType.Sha1, Function.identity());
  }

  /**
   * Bootstrap a new responseTimeInMillis check that extracts the response time of the request.
   *
   * <p>Note: On contrary to the Scala DSL, the compiler can't check the availability of this check
   * type for your protocol. If the protocol you're using doesn't support it, you'll get a runtime
   * {@link IllegalArgumentException}
   *
   * @return the next DSL step
   */
  @Nonnull
  public static CheckBuilder.Find<Integer> responseTimeInMillis() {
    return CoreCheckBuilders.responseTimeInMillis();
  }

  ////////// FeederSupport

  /**
   * Bootstrap a new <a href="https://datatracker.ietf.org/doc/html/rfc4180">CSV</a> file based
   * feeder
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.Batchable<String> csv(@Nonnull String filePath) {
    return FeederBuilder.Impl.csv(filePath);
  }

  /**
   * Bootstrap a new <a href="https://datatracker.ietf.org/doc/html/rfc4180">CSV</a> file based
   * feeder
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @param quoteChar the quote char to wrap values containing special characters
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.Batchable<String> csv(@Nonnull String filePath, char quoteChar) {
    return FeederBuilder.Impl.csv(filePath, quoteChar);
  }

  /**
   * Bootstrap a new <a href="https://datatracker.ietf.org/doc/html/rfc4180">CSV</a> file based
   * feeder, where the separator is a semi-colon
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.Batchable<String> ssv(@Nonnull String filePath) {
    return FeederBuilder.Impl.ssv(filePath);
  }

  /**
   * Bootstrap a new <a href="https://datatracker.ietf.org/doc/html/rfc4180">CSV</a> file based
   * feeder, where the separator is a semi-colon
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @param quoteChar the quote char to wrap values containing special characters
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.Batchable<String> ssv(@Nonnull String filePath, char quoteChar) {
    return FeederBuilder.Impl.ssv(filePath, quoteChar);
  }

  /**
   * Bootstrap a new <a href="https://datatracker.ietf.org/doc/html/rfc4180">CSV</a> file based
   * feeder, where the separator is a tab
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.Batchable<String> tsv(@Nonnull String filePath) {
    return FeederBuilder.Impl.tsv(filePath);
  }

  /**
   * Bootstrap a new <a href="https://datatracker.ietf.org/doc/html/rfc4180">CSV</a> file based
   * feeder, where the separator is a tab
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @param quoteChar the quote char to wrap values containing special characters
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.Batchable<String> tsv(@Nonnull String filePath, char quoteChar) {
    return FeederBuilder.Impl.tsv(filePath, quoteChar);
  }

  /**
   * Bootstrap a new <a href="https://datatracker.ietf.org/doc/html/rfc4180">CSV</a> file based
   * feeder, where the separator is provided
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @param separator the provided separator char
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.Batchable<String> separatedValues(
      @Nonnull String filePath, char separator) {
    return FeederBuilder.Impl.separatedValues(filePath, separator);
  }

  /**
   * Bootstrap a new <a href="https://datatracker.ietf.org/doc/html/rfc4180">CSV</a> file based
   * feeder, where the separator is provided
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @param separator the provided separator char
   * @param quoteChar the quote char to wrap values containing special characters
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.Batchable<String> separatedValues(
      @Nonnull String filePath, char separator, char quoteChar) {
    return FeederBuilder.Impl.separatedValues(filePath, separator, quoteChar);
  }

  /**
   * Bootstrap a new JSON file based feeder
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or absolute
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder.FileBased<Object> jsonFile(@Nonnull String filePath) {
    return FeederBuilder.Impl.jsonFile(filePath);
  }

  /**
   * Bootstrap a new JSON API based feeder
   *
   * @param url the url of the API
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder<Object> jsonUrl(String url) {
    return FeederBuilder.Impl.jsonUrl(url);
  }

  /**
   * Bootstrap a new in-memory array of Maps based feeder
   *
   * @param data the in-memory data
   * @return a new feeder
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  public static FeederBuilder<Object> arrayFeeder(@Nonnull Map<String, Object>[] data) {
    scala.collection.immutable.Map<String, Object>[] scalaArray =
        Arrays.stream(data)
            .map(Converters::toScalaMap)
            .toArray(scala.collection.immutable.Map[]::new);
    return new FeederBuilder.Impl<>(
        io.gatling.core.Predef.array2FeederBuilder(
            scalaArray, io.gatling.core.Predef.configuration()));
  }

  /**
   * Bootstrap a new in-memory List of Maps based feeder
   *
   * @param data the in-memory data
   * @return a new feeder
   */
  @Nonnull
  public static FeederBuilder<Object> listFeeder(@Nonnull List<Map<String, Object>> data) {
    scala.collection.immutable.Seq<scala.collection.immutable.Map<String, Object>> seq =
        toScalaSeq(data.stream().map(Converters::toScalaMap).collect(Collectors.toList()));
    return new FeederBuilder.Impl<>(
        io.gatling.core.Predef.seq2FeederBuilder(
            seq.toIndexedSeq(), io.gatling.core.Predef.configuration()));
  }

  //////////  OpenInjectionSupport

  /**
   * Bootstrap a new open workload rampUsers injection profile, see {@link OpenInjectionStep.Ramp}
   *
   * @param users the total number of users to inject
   * @return the next DSL step
   */
  @Nonnull
  public static OpenInjectionStep.Ramp rampUsers(int users) {
    return new OpenInjectionStep.Ramp(users);
  }

  /**
   * Bootstrap a new open workload stress peak injection profile, see {@link
   * OpenInjectionStep.StressPeak}
   *
   * @param users the total number of users to inject
   * @return the next DSL step
   */
  @Nonnull
  public static OpenInjectionStep.StressPeak stressPeakUsers(int users) {
    return new OpenInjectionStep.StressPeak(users);
  }

  /**
   * Bootstrap a new open workload atOnceUsers injection profile, see {@link
   * OpenInjectionStep#atOnceUsers(int)}
   *
   * @param users the total number of users to inject
   * @return the next DSL step
   */
  @Nonnull
  public static OpenInjectionStep atOnceUsers(int users) {
    return OpenInjectionStep.atOnceUsers(users);
  }

  /**
   * Bootstrap a new open workload constantUsersPerSec injection profile, see {@link
   * OpenInjectionStep.ConstantRate}
   *
   * @param rate the users per second rate
   * @return the next DSL step
   */
  @Nonnull
  public static OpenInjectionStep.ConstantRate constantUsersPerSec(double rate) {
    return new OpenInjectionStep.ConstantRate(rate);
  }

  /**
   * Bootstrap a new open workload rampUsersPerSec injection profile, see {@link
   * OpenInjectionStep.RampRate}
   *
   * @param rate the users per second start rate
   * @return the next DSL step
   */
  @Nonnull
  public static OpenInjectionStep.RampRate rampUsersPerSec(double rate) {
    return new OpenInjectionStep.RampRate(rate);
  }

  /**
   * Bootstrap a new open workload nothingFor injection profile, see {@link
   * OpenInjectionStep#nothingFor(Duration)}
   *
   * @param durationSeconds the duration in seconds
   * @return the next DSL step
   */
  @Nonnull
  public static OpenInjectionStep nothingFor(int durationSeconds) {
    return nothingFor(Duration.ofSeconds(durationSeconds));
  }

  /**
   * Bootstrap a new open workload nothingFor injection profile, see {@link
   * OpenInjectionStep#nothingFor(Duration)}
   *
   * @param duration the duration
   * @return the next DSL step
   */
  @Nonnull
  public static OpenInjectionStep nothingFor(@Nonnull Duration duration) {
    return OpenInjectionStep.nothingFor(duration);
  }

  /**
   * Bootstrap a new open workload incrementUsersPerSec injection profile, see {@link
   * OpenInjectionStep.Stairs}
   *
   * @param rateIncrement the difference of users per second rate between levels of the stairs
   * @return the next DSL step
   */
  @Nonnull
  public static OpenInjectionStep.Stairs incrementUsersPerSec(double rateIncrement) {
    return new OpenInjectionStep.Stairs(rateIncrement);
  }

  //////////  ClosedInjectionSupport
  /**
   * Bootstrap a new closed workload constantConcurrentUsers injection profile, see {@link
   * ClosedInjectionStep.Constant}
   *
   * @param users the number of concurrent users
   * @return the next DSL step
   */
  @Nonnull
  public static ClosedInjectionStep.Constant constantConcurrentUsers(int users) {
    return new ClosedInjectionStep.Constant(users);
  }

  /**
   * Bootstrap a new closed workload rampConcurrentUsers injection profile, see {@link
   * ClosedInjectionStep.Ramp}
   *
   * @param from the number of concurrent users at the start of the ramp
   * @return the next DSL step
   */
  @Nonnull
  public static ClosedInjectionStep.Ramp rampConcurrentUsers(int from) {
    return new ClosedInjectionStep.Ramp(from);
  }

  /**
   * Bootstrap a new closed workload incrementConcurrentUsers injection profile, see {@link
   * ClosedInjectionStep.Stairs}
   *
   * @param usersIncrement the difference of concurrent users between levels of the stairs
   * @return the next DSL step
   */
  @Nonnull
  public static ClosedInjectionStep.Stairs incrementConcurrentUsers(int usersIncrement) {
    return new ClosedInjectionStep.Stairs(usersIncrement);
  }

  //////////  ThrottlingSupport

  /**
   * Bootstrap a new reachRps throttling profile, see {@link ThrottleStep.ReachIntermediate}
   *
   * @param target the target requests per second
   * @return the next DSL step
   */
  @Nonnull
  public static ThrottleStep.ReachIntermediate reachRps(int target) {
    return new ThrottleStep.ReachIntermediate(target);
  }

  /**
   * Bootstrap a new holdFor throttling profile that limits rps to its current value
   *
   * @param duration the duration of the plateau in seconds
   * @return the next DSL step
   */
  @Nonnull
  public static ThrottleStep holdFor(int duration) {
    return holdFor(Duration.ofSeconds(duration));
  }

  /**
   * Bootstrap a new holdFor throttling profile that limits rps to its current value
   *
   * @param duration the duration of the plateau
   * @return the next DSL step
   */
  @Nonnull
  public static ThrottleStep holdFor(@Nonnull Duration duration) {
    return new ThrottleStep(
        new io.gatling.core.controller.throttle.Hold(toScalaDuration(duration)));
  }

  /**
   * Bootstrap a new jumpToRps throttling profile that change the rps limit to a new value
   *
   * @param target the new limit
   * @return the next DSL step
   */
  @Nonnull
  public static ThrottleStep jumpToRps(int target) {
    return new ThrottleStep(new io.gatling.core.controller.throttle.Jump(target));
  }

  //////////  AssertionSupport

  /**
   * Bootstrap a new global assertion that targets stats aggregated on all requests
   *
   * @return the next DSL step
   */
  @Nonnull
  public static Assertion.WithPath global() {
    return new Assertion.WithPath(Global$.MODULE$);
  }

  /**
   * Bootstrap a new forAll assertion that targets stats on each individual request type
   *
   * @return the next DSL step
   */
  @Nonnull
  public static Assertion.WithPath forAll() {
    return new Assertion.WithPath(ForAll$.MODULE$);
  }

  /**
   * Bootstrap a new details assertion that targets stats on a specific request type
   *
   * @return the next DSL step
   */
  @Nonnull
  public static Assertion.WithPath details(@Nonnull String... parts) {
    Seq<String> stringSeq = toScalaSeq(parts);
    return new Assertion.WithPath(new Details(stringSeq.toList()));
  }

  ////////// BodySupport
  /**
   * A function to pass to process the request body and compress it with GZIP before writing it on
   * the wire
   */
  public static final Function<Body, Body.WithBytes> gzipBody =
      javaBody ->
          new Body.WithBytes(io.gatling.core.body.BodyProcessors.gzip().apply(javaBody.asScala()));

  /**
   * Create a body from a String.
   *
   * <p>Can also be used as a Function<Session, String> to define the expected value in a check.
   *
   * @param string the body expressed as a gatling Expression Language String
   * @return a body
   */
  @Nonnull
  public static Body.WithString StringBody(@Nonnull String string) {
    return new Body.WithString(
        io.gatling.core.Predef.StringBody(string, io.gatling.core.Predef.configuration()));
  }

  /**
   * Create a body from a String.
   *
   * <p>Can also be used as a Function<Session, String> to define the expected value in a check.
   *
   * @param f the body expressed as a function
   * @return a body
   */
  @Nonnull
  public static Body.WithString StringBody(@Nonnull Function<Session, String> f) {
    return new Body.WithString(
        io.gatling.core.Predef.StringBody(
            javaFunctionToExpression(f), io.gatling.core.Predef.configuration()));
  }

  /**
   * Create a body from a file. Bytes will be sent without any transformation.
   *
   * <p>Can also be used as a Function<Session, byte[]> to define the expected value in a check.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return a body
   */
  @Nonnull
  public static Body.WithBytes RawFileBody(@Nonnull String filePath) {
    return new Body.WithBytes(
        io.gatling.core.Predef.RawFileBody(
            toStringExpression(filePath), io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Create a body from a file. Bytes will be sent without any transformation.
   *
   * <p>Can also be used as a Function<Session, byte[]> to define the expected value in a check.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return a body
   */
  @Nonnull
  public static Body.WithBytes RawFileBody(@Nonnull Function<Session, String> filePath) {
    return new Body.WithBytes(
        io.gatling.core.Predef.RawFileBody(
            javaFunctionToExpression(filePath), io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Create a body from a file. File text content will be processed as a Gatling Expression Language
   * String.
   *
   * <p>Can also be used as a Function<Session, String> to define the expected value in a check.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return a body
   */
  @Nonnull
  public static Body.WithString ElFileBody(@Nonnull String filePath) {
    return new Body.WithString(
        io.gatling.core.Predef.ElFileBody(
            toStringExpression(filePath), io.gatling.core.Predef.elFileBodies()));
  }

  /**
   * Create a body from a file. File text content will be processed as a Gatling Expression Language
   * String.
   *
   * <p>Can also be used as a Function<Session, String> to define the expected value in a check.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return a body
   */
  @Nonnull
  public static Body.WithString ElFileBody(@Nonnull Function<Session, String> filePath) {
    return new Body.WithString(
        io.gatling.core.Predef.ElFileBody(
            javaFunctionToExpression(filePath), io.gatling.core.Predef.elFileBodies()));
  }

  /**
   * Create a body from String processed as a <a href="https://pebbletemplates.io/">Pebble
   * template</a>.
   *
   * <p>Can also be used as a Function<Session, String> to define the expected value in a check.
   *
   * @param string the Pebble string
   * @return a body
   */
  @Nonnull
  public static Body.WithString PebbleStringBody(@Nonnull String string) {
    return new Body.WithString(
        io.gatling.core.Predef.PebbleStringBody(string, io.gatling.core.Predef.configuration()));
  }

  /**
   * Create a body from a file. File text content will be processed as a <a
   * href="https://pebbletemplates.io/">Pebble template</a>.
   *
   * <p>Can also be used as a Function<Session, String> to define the expected value in a check.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return a body
   */
  @Nonnull
  public static Body.WithString PebbleFileBody(@Nonnull String filePath) {
    return new Body.WithString(
        io.gatling.core.Predef.PebbleFileBody(
            toStringExpression(filePath),
            io.gatling.core.Predef.pebbleFileBodies(),
            io.gatling.core.Predef.configuration()));
  }

  /**
   * Create a body from a file. File text content will be processed as a <a
   * href="https://pebbletemplates.io/">Pebble template</a>.
   *
   * <p>Can also be used as a Function<Session, String> to define the expected value in a check.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return a body
   */
  @Nonnull
  public static Body.WithString PebbleFileBody(@Nonnull Function<Session, String> filePath) {
    return new Body.WithString(
        io.gatling.core.Predef.PebbleFileBody(
            javaFunctionToExpression(filePath),
            io.gatling.core.Predef.pebbleFileBodies(),
            io.gatling.core.Predef.configuration()));
  }

  /**
   * Create a body from a byte array. Bytes will be sent as is.
   *
   * <p>Can also be used as a Function<Session, byte[]> to define the expected value in a check.
   *
   * @param bytes the bytes
   * @return a body
   */
  @Nonnull
  public static Body.WithBytes ByteArrayBody(@Nonnull byte[] bytes) {
    return new Body.WithBytes(io.gatling.core.Predef.ByteArrayBody(toStaticValueExpression(bytes)));
  }

  /**
   * Create a body from a byte array. Bytes will be sent as is.
   *
   * <p>Can also be used as a Function<Session, byte[]> to define the expected value in a check.
   *
   * @param bytes the bytes, expressed as a Gatling Expression Language String
   * @return a body
   */
  @Nonnull
  public static Body.WithBytes ByteArrayBody(@Nonnull String bytes) {
    return new Body.WithBytes(io.gatling.core.Predef.ByteArrayBody(toBytesExpression(bytes)));
  }

  /**
   * Create a body from a byte array. Bytes will be sent as is.
   *
   * <p>Can also be used as a Function<Session, byte[]> to define the expected value in a check.
   *
   * @param bytes the bytes, expressed as a function
   * @return a body
   */
  @Nonnull
  public static Body.WithBytes ByteArrayBody(@Nonnull Function<Session, byte[]> bytes) {
    return new Body.WithBytes(
        io.gatling.core.Predef.ByteArrayBody(javaFunctionToExpression(bytes)));
  }

  /**
   * Create a body from a byte stream. Bytes will be sent as is.
   *
   * @param stream the bytes, expressed as a function
   * @return a body
   */
  @Nonnull
  public static Body InputStreamBody(@Nonnull Function<Session, InputStream> stream) {
    return new Body.Default(
        io.gatling.core.Predef.InputStreamBody(javaFunctionToExpression(stream)));
  }

  /**
   * Register some <a href="https://pebbletemplates.io/wiki/guide/extending-pebble/">custom Pebble
   * extensions</a>
   *
   * @param extensions the custom extensions
   */
  public static void registerPebbleExtensions(@Nonnull Extension... extensions) {
    io.gatling.core.Predef.registerPebbleExtensions(toScalaSeq(extensions));
  }
}
