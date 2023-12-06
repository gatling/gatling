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

package io.gatling.javaapi.core.error;

import static io.gatling.javaapi.core.internal.Expressions.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.errors.ScalaExitHereIf;
import io.gatling.javaapi.core.internal.errors.ScalaStopInjectorIf;
import io.gatling.javaapi.core.internal.errors.ScalaTryMax;
import java.util.UUID;
import java.util.function.Function;

/**
 * Methods for defining error handling components.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface Errors<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Define a block that is interrupted for a given virtual user if it experiences a failure.
   *
   * @param chain the chain to interrupt on error
   * @param chains other chains
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T exitBlockOnFail(@NonNull ChainBuilder chain, @NonNull ChainBuilder... chains) {
    return make(wrapped -> wrapped.exitBlockOnFail(chain.exec(chains).wrapped));
  }

  /**
   * Define a block that is interrupted and retried for a given virtual user if it experiences a
   * failure.
   *
   * @param times the maximum number of tries, including the first one (hence number of retries + 1)
   * @return a DSL component for defining the tried block
   */
  @NonNull
  default TryMax<T> tryMax(int times) {
    return tryMax(times, UUID.randomUUID().toString());
  }

  /**
   * Define a block that is interrupted and retried for a given virtual user if it experiences a
   * failure.
   *
   * @param times the maximum number of tries, including the first one (hence number of retries + 1)
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the tried block
   */
  @NonNull
  default TryMax<T> tryMax(int times, @NonNull String counterName) {
    return new TryMax<>(ScalaTryMax.apply(this, times, counterName));
  }

  /**
   * Define a block that is interrupted and retried for a given virtual user if it experiences a
   * failure.
   *
   * @param times the maximum number of tries, including the first one (hence number of retries +
   *     1), expressed as a Gatling Expression Language String
   * @return a DSL component for defining the tried block
   */
  @NonNull
  default TryMax<T> tryMax(String times) {
    return tryMax(times, UUID.randomUUID().toString());
  }

  /**
   * Define a block that is interrupted and retried for a given virtual user if it experiences a
   * failure.
   *
   * @param times the maximum number of tries, including the first one (hence number of retries +
   *     1), expressed as a Gatling Expression Language String
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the tried block
   */
  @NonNull
  default TryMax<T> tryMax(@NonNull String times, @NonNull String counterName) {
    return new TryMax<>(ScalaTryMax.apply(this, times, counterName));
  }

  /**
   * Define a block that is interrupted and retried for a given virtual user if it experiences a
   * failure.
   *
   * @param times the maximum number of tries, including the first one (hence number of retries +
   *     1), expressed as function
   * @return a DSL component for defining the tried block
   */
  @NonNull
  default TryMax<T> tryMax(@NonNull Function<Session, Integer> times) {
    return tryMax(times, UUID.randomUUID().toString());
  }

  /**
   * Define a block that is interrupted and retried for a given virtual user if it experiences a
   * failure.
   *
   * @param times the maximum number of tries, including the first one (hence number of retries +
   *     1), expressed as a function
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the tried block
   */
  @NonNull
  default TryMax<T> tryMax(@NonNull Function<Session, Integer> times, @NonNull String counterName) {
    return new TryMax<>(ScalaTryMax.apply(this, times, counterName));
  }

  /**
   * The DSL component for defining the tried block
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class TryMax<T extends StructureBuilder<T, ?>> {
    private final ScalaTryMax.Times<T, ?> wrapped;

    TryMax(ScalaTryMax.Times<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the tried block
     *
     * @param chain the loop content
     * @param chains other chains
     * @return a new {@link StructureBuilder}
     */
    @NonNull
    public T on(@NonNull ChainBuilder chain, @NonNull ChainBuilder... chains) {
      return wrapped.trying(chain.exec(chains));
    }
  }

  /**
   * Have the virtual user exit here if the condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T exitHereIf(@NonNull String condition) {
    return ScalaExitHereIf.apply(this, condition);
  }

  /**
   * Have the virtual user exit here if the condition holds true
   *
   * @param condition the condition, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T exitHereIf(@NonNull Function<Session, Boolean> condition) {
    return ScalaExitHereIf.apply(this, condition);
  }

  /**
   * Have the virtual user exit here
   *
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T exitHere() {
    return make(io.gatling.core.structure.Errors::exitHere);
  }

  /**
   * Have the virtual user exit here if the state of its Session is failed, see {@link
   * Session#isFailed()}
   *
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T exitHereIfFailed() {
    return make(io.gatling.core.structure.Errors::exitHereIfFailed);
  }

  /**
   * Have the virtual user abruptly stop the injector
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T stopInjector(String message) {
    return make(wrapped -> wrapped.stopInjector(toStringExpression(message)));
  }

  /**
   * Have the virtual user abruptly stop the injector
   *
   * @param message the message, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T stopInjector(Function<Session, String> message) {
    return make(wrapped -> wrapped.stopInjector(javaFunctionToExpression(message)));
  }

  /**
   * Have the virtual user abruptly stop the injector if a condition is met
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T stopInjectorIf(String message, @NonNull String condition) {
    return ScalaStopInjectorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly stop the injector if a condition is met
   *
   * @param message the message, expressed as a function
   * @param condition the condition, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T stopInjectorIf(
      Function<Session, String> message, @NonNull Function<Session, Boolean> condition) {
    return ScalaStopInjectorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly stop the injector if a condition is met
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @param condition the condition, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T stopInjectorIf(String message, @NonNull Function<Session, Boolean> condition) {
    return ScalaStopInjectorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly stop the injector if a condition is met
   *
   * @param message the message, expressed as a function
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  @NonNull
  default T stopInjectorIf(Function<Session, String> message, @NonNull String condition) {
    return ScalaStopInjectorIf.apply(this, message, condition);
  }
}
