/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import static io.gatling.javaapi.core.internal.Expressions.*;

import java.util.function.Function;
import org.jspecify.annotations.NonNull;

/**
 * Builder of an action that stores an incrementing value into the virtual users' Session, tracked
 * independently for each virtual user. Only reachable from {@link CounterBuilder#perUser()}, as
 * dynamic values, eg functions and Gatling EL Strings, only make sense when tracking per user: a
 * range resolved from one virtual user's Session can't define the sequence shared by all of them.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class PerUserCounterBuilder implements ActionBuilder {

  private final io.gatling.core.action.builder.PerUserCounterBuilder wrapped;

  PerUserCounterBuilder(io.gatling.core.action.builder.PerUserCounterBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Set the first value to be emitted. Must be positive. Default is 0.
   *
   * @param start the first value
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder startingAt(int start) {
    return new PerUserCounterBuilder(wrapped.startingAt(toStaticValueExpression(start)));
  }

  /**
   * Set the first value to be emitted. Must be positive. Default is 0.
   *
   * @param start the first value, as a Gatling EL String
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder startingAt(@NonNull String start) {
    return new PerUserCounterBuilder(wrapped.startingAt(toIntExpression(start)));
  }

  /**
   * Set the first value to be emitted. Must be positive. Default is 0.
   *
   * @param start the first value, as a function
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder startingAt(@NonNull Function<Session, Integer> start) {
    return new PerUserCounterBuilder(wrapped.startingAt(javaIntegerFunctionToExpression(start)));
  }

  /**
   * Set the gap between 2 successive values. Default is 1.
   *
   * @param increment the gap between 2 successive values
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder withIncrement(int increment) {
    return new PerUserCounterBuilder(wrapped.withIncrement(toStaticValueExpression(increment)));
  }

  /**
   * Set the gap between 2 successive values. Default is 1.
   *
   * @param increment the gap between 2 successive values, as a Gatling EL String
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder withIncrement(@NonNull String increment) {
    return new PerUserCounterBuilder(wrapped.withIncrement(toIntExpression(increment)));
  }

  /**
   * Set the gap between 2 successive values. Default is 1.
   *
   * @param increment the gap between 2 successive values, as a function
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder withIncrement(
      @NonNull Function<Session, Integer> increment) {
    return new PerUserCounterBuilder(
        wrapped.withIncrement(javaIntegerFunctionToExpression(increment)));
  }

  /**
   * Set the upper bound, exclusive, except when it's Integer.MAX_VALUE, which is the default. Once
   * it's reached, the load generator is stopped, unless {@link #wrapAround()} is used.
   *
   * @param end the exclusive upper bound
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder upTo(int end) {
    return new PerUserCounterBuilder(wrapped.upTo(toStaticValueExpression(end)));
  }

  /**
   * Set the upper bound, exclusive, except when it's Integer.MAX_VALUE, which is the default. Once
   * it's reached, the load generator is stopped, unless {@link #wrapAround()} is used.
   *
   * @param end the exclusive upper bound, as a Gatling EL String
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder upTo(@NonNull String end) {
    return new PerUserCounterBuilder(wrapped.upTo(toIntExpression(end)));
  }

  /**
   * Set the upper bound, exclusive, except when it's Integer.MAX_VALUE, which is the default. Once
   * it's reached, the load generator is stopped, unless {@link #wrapAround()} is used.
   *
   * @param end the exclusive upper bound, as a function
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder upTo(@NonNull Function<Session, Integer> end) {
    return new PerUserCounterBuilder(wrapped.upTo(javaIntegerFunctionToExpression(end)));
  }

  /**
   * Start over from the first value once the upper bound is reached, instead of stopping the load
   * generator. Beware values are then no longer unique.
   *
   * @return a new PerUserCounterBuilder
   */
  public @NonNull PerUserCounterBuilder wrapAround() {
    return new PerUserCounterBuilder(wrapped.wrapAround());
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped;
  }
}
