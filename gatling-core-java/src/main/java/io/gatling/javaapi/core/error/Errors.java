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

package io.gatling.javaapi.core.error;

import static io.gatling.javaapi.core.internal.Expressions.*;

import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.exec.Executable;
import io.gatling.javaapi.core.internal.Executables;
import io.gatling.javaapi.core.internal.errors.ScalaCrashLoadGeneratorIf;
import io.gatling.javaapi.core.internal.errors.ScalaExitBlockOnFail;
import io.gatling.javaapi.core.internal.errors.ScalaExitHereIf;
import io.gatling.javaapi.core.internal.errors.ScalaStopLoadGeneratorIf;
import io.gatling.javaapi.core.internal.errors.ScalaTryMax;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

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
   * @return a DSL component for defining the tried block
   */
  default @NonNull ExitBlockOnFail<T> exitBlockOnFail() {
    return new ExitBlockOnFail<>(new ScalaExitBlockOnFail<>(this));
  }

  /**
   * Define a block that is interrupted for a given virtual user if it experiences a failure.
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class ExitBlockOnFail<T extends StructureBuilder<T, ?>> {

    private final ScalaExitBlockOnFail<T, ?> context;

    private ExitBlockOnFail(ScalaExitBlockOnFail<T, ?> context) {
      this.context = context;
    }

    /**
     * Define the tried block
     *
     * @param executable the chain to interrupt on error
     * @param executables other chains
     * @return a new {@link StructureBuilder}
     */
    public @NonNull T on(@NonNull Executable executable, @NonNull Executable... executables) {
      return context.exitBlockOnFail(Executables.toChainBuilder(executable, executables));
    }
  }

  /**
   * Define a block that is interrupted and retried for a given virtual user if it experiences a
   * failure.
   *
   * @param times the maximum number of tries, including the first one (hence number of retries + 1)
   * @return a DSL component for defining the tried block
   */
  default @NonNull TryMax<T> tryMax(int times) {
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
  default @NonNull TryMax<T> tryMax(int times, @NonNull String counterName) {
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
  default @NonNull TryMax<T> tryMax(String times) {
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
  default @NonNull TryMax<T> tryMax(@NonNull String times, @NonNull String counterName) {
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
  default @NonNull TryMax<T> tryMax(@NonNull Function<Session, Integer> times) {
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
  default @NonNull TryMax<T> tryMax(
      @NonNull Function<Session, Integer> times, @NonNull String counterName) {
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
     * @param executable the loop content
     * @param executables other chains
     * @return a new {@link StructureBuilder}
     */
    public @NonNull T on(@NonNull Executable executable, @NonNull Executable... executables) {
      return wrapped.trying(Executables.toChainBuilder(executable, executables));
    }
  }

  /**
   * Have the virtual user exit here if the condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T exitHereIf(@NonNull String condition) {
    return ScalaExitHereIf.apply(this, condition);
  }

  /**
   * Have the virtual user exit here if the condition holds true
   *
   * @param condition the condition, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T exitHereIf(@NonNull Function<Session, Boolean> condition) {
    return ScalaExitHereIf.apply(this, condition);
  }

  /**
   * Have the virtual user exit here
   *
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T exitHere() {
    return make(io.gatling.core.structure.Errors::exitHere);
  }

  /**
   * Have the virtual user exit here if the state of its Session is failed, see {@link
   * Session#isFailed()}
   *
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T exitHereIfFailed() {
    return make(io.gatling.core.structure.Errors::exitHereIfFailed);
  }

  /**
   * Have the virtual user abruptly stop the load generator with a successful status
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T stopLoadGenerator(String message) {
    return make(wrapped -> wrapped.stopLoadGenerator(toStringExpression(message)));
  }

  /**
   * Have the virtual user abruptly stop the load generator with a successful status
   *
   * @param message the message, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T stopLoadGenerator(Function<Session, String> message) {
    return make(wrapped -> wrapped.stopLoadGenerator(javaFunctionToExpression(message)));
  }

  /**
   * Have the virtual user abruptly stop the load generator with a successful status if a condition
   * is met
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T stopLoadGeneratorIf(String message, @NonNull String condition) {
    return ScalaStopLoadGeneratorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly stop the load generator with a successful status if a condition
   * is met
   *
   * @param message the message, expressed as a function
   * @param condition the condition, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T stopLoadGeneratorIf(
      Function<Session, String> message, @NonNull Function<Session, Boolean> condition) {
    return ScalaStopLoadGeneratorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly stop the load generator with a successful status if a condition
   * is met
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @param condition the condition, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T stopLoadGeneratorIf(
      String message, @NonNull Function<Session, Boolean> condition) {
    return ScalaStopLoadGeneratorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly stop the load generator with a successful status if a condition
   * is met
   *
   * @param message the message, expressed as a function
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T stopLoadGeneratorIf(
      Function<Session, String> message, @NonNull String condition) {
    return ScalaStopLoadGeneratorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly stop the load generator with a failed status
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T crashLoadGenerator(String message) {
    return make(wrapped -> wrapped.crashLoadGenerator(toStringExpression(message)));
  }

  /**
   * Have the virtual user abruptly crash the load generator with a failed status
   *
   * @param message the message, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T crashLoadGenerator(Function<Session, String> message) {
    return make(wrapped -> wrapped.crashLoadGenerator(javaFunctionToExpression(message)));
  }

  /**
   * Have the virtual user abruptly crash the load generator with a failed status if a condition is
   * met
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T crashLoadGeneratorIf(String message, @NonNull String condition) {
    return ScalaCrashLoadGeneratorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly crash the load generator with a failed status if a condition is
   * met
   *
   * @param message the message, expressed as a function
   * @param condition the condition, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T crashLoadGeneratorIf(
      Function<Session, String> message, @NonNull Function<Session, Boolean> condition) {
    return ScalaCrashLoadGeneratorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly crash the load generator with a failed status if a condition is
   * met
   *
   * @param message the message, expressed as a Gatling Expression Language String
   * @param condition the condition, expressed as a function
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T crashLoadGeneratorIf(
      String message, @NonNull Function<Session, Boolean> condition) {
    return ScalaCrashLoadGeneratorIf.apply(this, message, condition);
  }

  /**
   * Have the virtual user abruptly crash the load generator with a failed status if a condition is
   * met
   *
   * @param message the message, expressed as a function
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a new {@link StructureBuilder}
   */
  default @NonNull T crashLoadGeneratorIf(
      Function<Session, String> message, @NonNull String condition) {
    return ScalaCrashLoadGeneratorIf.apply(this, message, condition);
  }
}
