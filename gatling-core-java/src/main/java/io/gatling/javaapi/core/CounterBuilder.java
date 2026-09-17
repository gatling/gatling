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
 * Builder of an action that stores an incrementing value into the virtual users' Session.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class CounterBuilder implements ActionBuilder {

  private final io.gatling.core.action.builder.CounterBuilder wrapped;

  public CounterBuilder(@NonNull String key) {
    this(io.gatling.core.action.builder.CounterBuilder$.MODULE$.apply(key));
  }

  private CounterBuilder(io.gatling.core.action.builder.CounterBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Set the first value to be emitted. Must be positive. Default is 0.
   *
   * @param start the first value
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder startingAt(int start) {
    return new CounterBuilder(wrapped.startingAt(toStaticValueExpression(start)));
  }

  /**
   * Set the first value to be emitted. Must be positive. Default is 0. Only supported when {@link
   * #perUser()} is used, otherwise building the Simulation will fail.
   *
   * @param start the first value, as a Gatling EL String
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder startingAt(@NonNull String start) {
    return new CounterBuilder(wrapped.startingAt(toIntExpression(start)));
  }

  /**
   * Set the first value to be emitted. Must be positive. Default is 0. Only supported when {@link
   * #perUser()} is used, otherwise building the Simulation will fail.
   *
   * @param start the first value, as a function
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder startingAt(@NonNull Function<Session, Integer> start) {
    return new CounterBuilder(wrapped.startingAt(javaIntegerFunctionToExpression(start)));
  }

  /**
   * Set the gap between 2 successive values. Default is 1.
   *
   * @param increment the gap between 2 successive values
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder withIncrement(int increment) {
    return new CounterBuilder(wrapped.withIncrement(toStaticValueExpression(increment)));
  }

  /**
   * Set the gap between 2 successive values. Default is 1. Only supported when {@link #perUser()}
   * is used, otherwise building the Simulation will fail.
   *
   * @param increment the gap between 2 successive values, as a Gatling EL String
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder withIncrement(@NonNull String increment) {
    return new CounterBuilder(wrapped.withIncrement(toIntExpression(increment)));
  }

  /**
   * Set the gap between 2 successive values. Default is 1. Only supported when {@link #perUser()}
   * is used, otherwise building the Simulation will fail.
   *
   * @param increment the gap between 2 successive values, as a function
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder withIncrement(@NonNull Function<Session, Integer> increment) {
    return new CounterBuilder(wrapped.withIncrement(javaIntegerFunctionToExpression(increment)));
  }

  /**
   * Set the inclusive upper bound. Default is Integer.MAX_VALUE. Once it's reached, the load
   * generator is stopped, unless {@link #wrapAround()} is used.
   *
   * @param end the inclusive upper bound
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder upTo(int end) {
    return new CounterBuilder(wrapped.upTo(toStaticValueExpression(end)));
  }

  /**
   * Set the inclusive upper bound. Default is Integer.MAX_VALUE. Once it's reached, the load
   * generator is stopped, unless {@link #wrapAround()} is used. Only supported when {@link
   * #perUser()} is used, otherwise building the Simulation will fail.
   *
   * @param end the inclusive upper bound, as a Gatling EL String
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder upTo(@NonNull String end) {
    return new CounterBuilder(wrapped.upTo(toIntExpression(end)));
  }

  /**
   * Set the inclusive upper bound. Default is Integer.MAX_VALUE. Once it's reached, the load
   * generator is stopped, unless {@link #wrapAround()} is used. Only supported when {@link
   * #perUser()} is used, otherwise building the Simulation will fail.
   *
   * @param end the inclusive upper bound, as a function
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder upTo(@NonNull Function<Session, Integer> end) {
    return new CounterBuilder(wrapped.upTo(javaIntegerFunctionToExpression(end)));
  }

  /**
   * Start over from the first value once the upper bound is reached, instead of stopping the load
   * generator. Beware values are then no longer unique.
   *
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder wrapAround() {
    return new CounterBuilder(wrapped.wrapAround());
  }

  /**
   * Track the counter independently for each virtual user, so they all get the very same sequence
   * of values, instead of sharing one single sequence.
   *
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder perUser() {
    return new CounterBuilder(wrapped.perUser());
  }

  /**
   * Distribute the values evenly amongst all the load generators of a Gatling Enterprise cluster,
   * so they remain unique cluster wide. Only effective when the test is running with Gatling
   * Enterprise, noop otherwise. Each load generator only gets a slice of the range, so the values
   * emitted by the whole cluster have holes.
   *
   * @return a new CounterBuilder
   */
  public @NonNull CounterBuilder shard() {
    return new CounterBuilder(wrapped.shard());
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped;
  }
}
