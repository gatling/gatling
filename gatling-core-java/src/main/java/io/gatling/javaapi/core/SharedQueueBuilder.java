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
 * A factory of actions that exchange values over an in-memory queue, so virtual users can
 * communicate with each other.
 *
 * <p>The queue is owned by this very instance, so it must be stored in a field that's then used
 * everywhere the queue must be accessed.
 *
 * <p>The queue is local to this load generator: when running a distributed test with Gatling
 * Enterprise, each load generator has its own queue and they don't exchange values with each other.
 */
public final class SharedQueueBuilder {

  private final io.gatling.core.action.builder.SharedQueueBuilder wrapped;

  public SharedQueueBuilder(@NonNull String name) {
    this.wrapped = io.gatling.core.action.builder.SharedQueueBuilder$.MODULE$.apply(name);
  }

  private static ActionBuilder toJava(io.gatling.core.action.builder.ActionBuilder scala) {
    return () -> scala;
  }

  /**
   * Define an action that stores a value into this queue, then moves on without waiting.
   *
   * @param value the value to be enqueued, expressed as a Gatling Expression Language String
   * @return an ActionBuilder
   */
  public @NonNull ActionBuilder put(@NonNull String value) {
    return toJava(wrapped.put(toAnyExpression(value)));
  }

  /**
   * Define an action that stores a value into this queue, then moves on without waiting.
   *
   * @param value the static value to be enqueued
   * @return an ActionBuilder
   */
  public @NonNull ActionBuilder put(@NonNull Object value) {
    return toJava(wrapped.put(toStaticValueExpression(value)));
  }

  /**
   * Define an action that stores a value into this queue, then moves on without waiting.
   *
   * @param value the value to be enqueued, expressed as a function
   * @return an ActionBuilder
   */
  public @NonNull ActionBuilder put(@NonNull Function<Session, Object> value) {
    return toJava(wrapped.put(javaObjectFunctionToExpression(value::apply)));
  }

  /**
   * Define an action that pops the oldest value of this queue into the virtual user's Session. If
   * the queue is empty, the virtual user waits until a value is available, possibly forever unless
   * {@link SharedQueueTakeBuilder#timeout(java.time.Duration)} is used.
   *
   * @param key the name of the Session attribute the value is stored into
   * @return a new SharedQueueTakeBuilder
   */
  public @NonNull SharedQueueTakeBuilder take(@NonNull String key) {
    return new SharedQueueTakeBuilder(wrapped.take(key));
  }

  /**
   * Define an action that pops the oldest value of this queue into the virtual user's Session. If
   * the queue is empty, the virtual user is marked as failed and moves on instead of waiting, and
   * the Session attribute is left untouched.
   *
   * @param key the name of the Session attribute the value is stored into
   * @return an ActionBuilder
   */
  public @NonNull ActionBuilder poll(@NonNull String key) {
    return toJava(wrapped.poll(key));
  }

  /**
   * Define an action that stores the current number of values in this queue into the virtual user's
   * Session.
   *
   * @param key the name of the Session attribute the size is stored into
   * @return an ActionBuilder
   */
  public @NonNull ActionBuilder size(@NonNull String key) {
    return toJava(wrapped.size(key));
  }
}
