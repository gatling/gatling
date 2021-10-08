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

package io.gatling.javaapi.core.loop;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.loop.ScalaDoWhileDuring;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Methods for defining "doWhileDuring" loops. Similar to {@link AsLongAsDuring} except the
 * condition is evaluated at the end of the loop.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface DoWhileDuring<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  // Gatling EL condition
  /**
   * Define a loop that will iterate as long as the condition holds true and a maximum duration
   * isn't reached. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param duration the maximum duration, expressed as a Gatling Expression Language String that
   *     must either evaluate to an {@link Integer} (seconds then) or a {@link Duration}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default Loop<T> doWhileDuring(@Nonnull String condition, @Nonnull String duration) {
    return doWhileDuring(condition, duration, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate as long as the condition holds true and a maximum duration
   * isn't reached. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param duration the maximum duration, expressed as a Gatling Expression Language String that
   *     must either evaluate to an {@link Integer} (seconds then) or a {@link Duration}
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default Loop<T> doWhileDuring(
      @Nonnull String condition, @Nonnull String duration, @Nonnull String counterName) {
    return doWhileDuring(condition, duration, counterName, false);
  }

  /**
   * Define a loop that will iterate as long as the condition holds true and a maximum duration
   * isn't reached. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param duration the maximum duration, expressed as a Gatling Expression Language String that
   *     must either evaluate to an {@link Integer} (seconds then) or a {@link Duration}
   * @param exitASAP if the loop must be interrupted if the condition becomes false or the maximum
   *     duration inside the loop
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default Loop<T> doWhileDuring(
      @Nonnull String condition, @Nonnull String duration, boolean exitASAP) {
    return doWhileDuring(condition, duration, UUID.randomUUID().toString(), exitASAP);
  }

  /**
   * Define a loop that will iterate as long as the condition holds true and a maximum duration
   * isn't reached. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param duration the maximum duration, expressed as a Gatling Expression Language String that
   *     must either evaluate to an {@link Integer} (seconds then) or a {@link Duration}
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @param exitASAP if the loop must be interrupted if the condition becomes false or the maximum
   *     duration inside the loop
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default Loop<T> doWhileDuring(
      @Nonnull String condition,
      @Nonnull String duration,
      @Nonnull String counterName,
      boolean exitASAP) {
    return new Loop<>(ScalaDoWhileDuring.apply(this, condition, duration, counterName, exitASAP));
  }

  // Function condition
  /**
   * Define a loop that will iterate as long as the condition holds true and a maximum duration
   * isn't reached. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a function
   * @param duration the maximum duration, expressed as a function
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default Loop<T> doWhileDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration) {
    return doWhileDuring(condition, duration, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate as long as the condition holds true and a maximum duration
   * isn't reached. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a function
   * @param duration the maximum duration, expressed as a function
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default Loop<T> doWhileDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      @Nonnull String counterName) {
    return doWhileDuring(condition, duration, counterName, false);
  }

  /**
   * Define a loop that will iterate as long as the condition holds true and a maximum duration
   * isn't reached. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a function
   * @param duration the maximum duration, expressed as a function
   * @param exitASAP if the loop must be interrupted if the condition becomes false or the maximum
   *     duration inside the loop
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default Loop<T> doWhileDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      boolean exitASAP) {
    return doWhileDuring(condition, duration, UUID.randomUUID().toString(), exitASAP);
  }

  /**
   * Define a loop that will iterate as long as the condition holds true and a maximum duration
   * isn't reached. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a function
   * @param duration the maximum duration, expressed as a function
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @param exitASAP if the loop must be interrupted if the condition becomes false or the maximum
   *     duration inside the loop
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default Loop<T> doWhileDuring(
      @Nonnull Function<Session, Boolean> condition,
      @Nonnull Function<Session, Duration> duration,
      @Nonnull String counterName,
      boolean exitASAP) {
    return new Loop<>(ScalaDoWhileDuring.apply(this, condition, duration, counterName, exitASAP));
  }

  /**
   * A DSL component for defining the loop content
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class Loop<T extends StructureBuilder<T, ?>> {
    private final ScalaDoWhileDuring.Loop<T, ?> wrapped;

    Loop(ScalaDoWhileDuring.Loop<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the loop content
     *
     * @param chain the loop content
     * @return a new {@link StructureBuilder}
     */
    @Nonnull
    public T loop(@Nonnull ChainBuilder chain) {
      return wrapped.loop(chain);
    }
  }
}
