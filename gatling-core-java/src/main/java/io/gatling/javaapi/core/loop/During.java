/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.loop.ScalaDuring;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

/**
 * Methods for defining "during" loops that iterate over a block for a maximum duration.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface During<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /////////////// long duration
  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a number of seconds
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(long duration) {
    return during(Duration.ofSeconds(duration));
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a number of seconds
   * @param exitASAP if the loop must be interrupted if the max duration is reached inside the loop
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(long duration, boolean exitASAP) {
    return during(Duration.ofSeconds(duration), exitASAP);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a number of seconds
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(long duration, String counterName) {
    return during(Duration.ofSeconds(duration), counterName);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a number of seconds
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @param exitASAP if the loop must be interrupted if the max duration is reached inside the loop
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(long duration, String counterName, boolean exitASAP) {
    return during(Duration.ofSeconds(duration), counterName, exitASAP);
  }

  /////////////// Duration duration
  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull Duration duration) {
    return during(unused -> duration);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration
   * @param exitASAP if the loop must be interrupted if the max duration is reached inside the loop
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull Duration duration, boolean exitASAP) {
    return during(unused -> duration, exitASAP);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull Duration duration, @NonNull String counterName) {
    return during(unused -> duration, counterName, true);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @param exitASAP if the loop must be interrupted if the max duration is reached inside the loop
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull Duration duration, @NonNull String counterName, boolean exitASAP) {
    return new On<>(ScalaDuring.apply(this, duration, counterName, exitASAP));
  }

  /////////////// Gatling EL duration
  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a Gatling Expression Language String that
   *     must either evaluate to an {@link Integer} (seconds then) or a {@link Duration}
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull String duration) {
    return during(duration, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a Gatling Expression Language String that
   *     must either evaluate to an {@link Integer} (seconds then) or a {@link Duration}
   * @param exitASAP if the loop must be interrupted if the max duration is reached inside the loop
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull String duration, boolean exitASAP) {
    return during(duration, UUID.randomUUID().toString(), exitASAP);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a Gatling Expression Language String that
   *     must either evaluate to an {@link Integer} (seconds then) or a {@link Duration}
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull String duration, @NonNull String counterName) {
    return during(duration, counterName, true);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a Gatling Expression Language String
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @param exitASAP if the loop must be interrupted if the max duration is reached inside the loop
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull String duration, @NonNull String counterName, boolean exitASAP) {
    return new On<>(ScalaDuring.apply(this, duration, counterName, exitASAP));
  }

  /////////////// Function duration
  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a function
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull Function<Session, Duration> duration) {
    return during(duration, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a function
   * @param exitASAP if the loop must be interrupted if the max duration is reached inside the loop
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull Function<Session, Duration> duration, boolean exitASAP) {
    return during(duration, UUID.randomUUID().toString(), exitASAP);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a function
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(@NonNull Function<Session, Duration> duration, @NonNull String counterName) {
    return during(duration, counterName, true);
  }

  /**
   * Define a loop that will iterate for a given duration. The condition is evaluated at the end of
   * the loop.
   *
   * @param duration the maximum duration, expressed as a function
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @param exitASAP if the loop must be interrupted if the max duration is reached inside the loop
   * @return a DSL component for defining the loop content
   */
  @NonNull
  default On<T> during(
      @NonNull Function<Session, Duration> duration,
      @NonNull String counterName,
      boolean exitASAP) {
    return new On<>(ScalaDuring.apply(this, duration, counterName, exitASAP));
  }

  /**
   * A DSL component for defining the loop content
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class On<T extends StructureBuilder<T, ?>> {
    private final ScalaDuring.Loop<T, ?> wrapped;

    On(ScalaDuring.Loop<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the loop content
     *
     * @param chain the loop content
     * @param chains other chains
     * @return a new {@link StructureBuilder}
     */
    @NonNull
    public T on(@NonNull ChainBuilder chain, @NonNull ChainBuilder... chains) {
      return wrapped.loop(chain.exec(chains));
    }
  }
}
