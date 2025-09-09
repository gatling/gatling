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

package io.gatling.javaapi.core.pause;

import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;

import io.gatling.javaapi.core.PauseType;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

/**
 * Pause methods for defining pause/think time steps in a Scenario.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface Pauses<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Attach a pause
   *
   * @param duration the pause duration in seconds
   * @return a new StructureBuilder
   */
  default @NonNull T pause(long duration) {
    return pause(Duration.ofSeconds(duration));
  }

  /**
   * Attach a pause
   *
   * @param duration the pause duration in seconds
   * @param pauseType the type of pause
   * @return a new StructureBuilder
   */
  default @NonNull T pause(long duration, @NonNull PauseType pauseType) {
    return pause(Duration.ofSeconds(duration), pauseType);
  }

  /**
   * Attach a pause
   *
   * @param duration the pause duration
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull Duration duration) {
    return make(wrapped -> wrapped.pause(toScalaDuration(duration)));
  }

  /**
   * Attach a pause
   *
   * @param duration the pause duration
   * @param pauseType the type of pause
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull Duration duration, @NonNull PauseType pauseType) {
    return make(wrapped -> wrapped.pause(toScalaDuration(duration), pauseType.asScala()));
  }

  /**
   * Attach a pause as a Gatling Expression Language string. This expression must resolve to either
   * an {@link Integer}, then the unit will be seconds, or a {@link Duration}.
   *
   * @param duration the pause duration as a Gatling Expression Language string
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull String duration) {
    return make(wrapped -> wrapped.pause(duration));
  }

  /**
   * Attach a pause as a Gatling Expression Language string. This expression must resolve to either
   * an {@link Integer}, then the unit will be seconds, or a {@link Duration}.
   *
   * @param duration the pause duration as a Gatling Expression Language string
   * @param pauseType the type of pause
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull String duration, @NonNull PauseType pauseType) {
    return make(wrapped -> wrapped.pause(duration, pauseType.asScala()));
  }

  /**
   * Attach a pause as a function
   *
   * @param f the pause duration as a function
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull Function<Session, Duration> f) {
    return make(wrapped -> wrapped.pause(javaDurationFunctionToExpression(f)));
  }

  /**
   * Attach a pause as a function
   *
   * @param f the pause duration as a function
   * @param pauseType the type of pause
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull Function<Session, Duration> f, @NonNull PauseType pauseType) {
    return make(wrapped -> wrapped.pause(javaDurationFunctionToExpression(f), pauseType.asScala()));
  }

  /**
   * Attach a pause computed randomly between 2 values in seconds
   *
   * @param min the pause minimum in seconds
   * @param max the pause maximum in seconds
   * @return a new StructureBuilder
   */
  default @NonNull T pause(long min, long max) {
    return pause(Duration.ofSeconds(min), Duration.ofSeconds(max));
  }

  /**
   * Attach a pause computed randomly between 2 values in seconds
   *
   * @param min the pause minimum in seconds
   * @param max the pause maximum in seconds
   * @param pauseType the type of pause
   * @return a new StructureBuilder
   */
  default @NonNull T pause(long min, long max, @NonNull PauseType pauseType) {
    return pause(Duration.ofSeconds(min), Duration.ofSeconds(max), pauseType);
  }

  /**
   * Attach a pause computed randomly between 2 values
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull Duration min, @NonNull Duration max) {
    return make(wrapped -> wrapped.pause(toScalaDuration(min), toScalaDuration(max)));
  }

  /**
   * Attach a pause computed randomly between 2 values
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @param pauseType the type of pause
   * @return a new StructureBuilder
   */
  default @NonNull T pause(
      @NonNull Duration min, @NonNull Duration max, @NonNull PauseType pauseType) {
    return make(
        wrapped -> wrapped.pause(toScalaDuration(min), toScalaDuration(max), pauseType.asScala()));
  }

  /**
   * Attach a pause computed randomly between 2 values as a Gatling Expression Language string.
   * Those expressions must resolve to either {@link Integer}s, then the unit will be seconds, or a
   * {@link Duration}.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull String min, @NonNull String max) {
    return make(wrapped -> wrapped.pause(min, max, TimeUnit.SECONDS));
  }

  /**
   * Attach a pause computed randomly between 2 values as a Gatling Expression Language string.
   * Those expressions must resolve to either {@link Integer}s, then the unit will be seconds, or a
   * {@link Duration}.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @param pauseType the type of pause
   * @return a new StructureBuilder
   */
  default @NonNull T pause(@NonNull String min, @NonNull String max, @NonNull PauseType pauseType) {
    return make(wrapped -> wrapped.pause(min, max, pauseType.asScala()));
  }

  /**
   * Attach a pause computed randomly between 2 values as functions.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @return a new StructureBuilder
   */
  default @NonNull T pause(
      @NonNull Function<Session, Duration> min, @NonNull Function<Session, Duration> max) {
    return make(
        wrapped ->
            wrapped.pause(
                javaDurationFunctionToExpression(min), javaDurationFunctionToExpression(max)));
  }

  /**
   * Attach a pause computed randomly between 2 values as functions.
   *
   * @param min the pause minimum
   * @param max the pause maximum
   * @param pauseType the type of pause
   * @return a new StructureBuilder
   */
  default @NonNull T pause(
      @NonNull Function<Session, Duration> min,
      @NonNull Function<Session, Duration> max,
      @NonNull PauseType pauseType) {
    return make(
        wrapped ->
            wrapped.pause(
                javaDurationFunctionToExpression(min),
                javaDurationFunctionToExpression(max),
                pauseType.asScala()));
  }
}
