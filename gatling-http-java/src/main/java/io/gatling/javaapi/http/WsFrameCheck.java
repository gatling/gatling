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

package io.gatling.javaapi.http;

import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.internal.ScalaWsFrameCheckBinaryConditions;
import io.gatling.javaapi.http.internal.ScalaWsFrameCheckTextConditions;
import io.gatling.javaapi.http.internal.WsChecks;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

/**
 * DSL for building WebSocket checks
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public abstract class WsFrameCheck {

  private WsFrameCheck() {}

  public abstract io.gatling.http.check.ws.WsFrameCheck asScala();

  /**
   * DSL for building WebSocket BINARY frames checks
   *
   * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
   */
  public static final class Binary extends WsFrameCheck {
    private final io.gatling.http.check.ws.WsFrameCheck.Binary wrapped;

    public Binary(io.gatling.http.check.ws.WsFrameCheck.Binary wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define conditions that have to hold true to match inbound messages and apply the checks on
     * them
     *
     * @param newMatchConditions the conditions to match
     * @return a new Binary instance
     */
    public @NonNull Binary matching(@NonNull CheckBuilder... newMatchConditions) {
      return matching(Arrays.asList(newMatchConditions));
    }

    /**
     * Define conditions that have to hold true to match inbound messages and apply the checks on
     * them
     *
     * @param newMatchConditions the conditions to match
     * @return a new Binary instance
     */
    public @NonNull Binary matching(@NonNull List<CheckBuilder> newMatchConditions) {
      return new Binary(wrapped.matching(WsChecks.toScalaBinaryChecks(newMatchConditions)));
    }

    /**
     * Define the checks to apply on inbound messages
     *
     * @param checks the checks
     * @return a new Binary instance
     */
    public @NonNull Binary check(@NonNull CheckBuilder... checks) {
      return check(Arrays.asList(checks));
    }

    /**
     * Define the checks to apply on inbound messages
     *
     * @param checks the checks
     * @return a new Binary instance
     */
    public @NonNull Binary check(@NonNull List<CheckBuilder> checks) {
      return new Binary(wrapped.check(WsChecks.toScalaBinaryChecks(checks)));
    }

    /**
     * Define the checks to apply on inbound messages when a condition holds true.
     *
     * @param condition a condition, expressed as a function
     * @return the next DSL step
     */
    public UntypedCondition checkIf(Function<Session, Boolean> condition) {
      return new UntypedCondition(ScalaWsFrameCheckBinaryConditions.untyped(wrapped, condition));
    }

    /**
     * Define the checks to apply on inbound messages when a condition holds true.
     *
     * @param condition a condition, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    public UntypedCondition checkIf(String condition) {
      return new UntypedCondition(ScalaWsFrameCheckBinaryConditions.untyped(wrapped, condition));
    }

    public static final class UntypedCondition {
      private final ScalaWsFrameCheckBinaryConditions.Untyped wrapped;

      public UntypedCondition(ScalaWsFrameCheckBinaryConditions.Untyped wrapped) {
        this.wrapped = wrapped;
      }

      /**
       * Define the checks to apply on inbound messages when a condition holds true.
       *
       * @param thenChecks the checks
       * @return a new Binary instance
       */
      public Binary then(CheckBuilder... thenChecks) {
        return then(Arrays.asList(thenChecks));
      }

      /**
       * Define the checks to apply when the condition holds true.
       *
       * @param thenChecks the checks
       * @return a new Binary instance
       */
      public Binary then(List<CheckBuilder> thenChecks) {
        return wrapped.then_(thenChecks);
      }
    }

    /**
     * Define the checks to apply on inbound messages when a condition holds true.
     *
     * @param condition a condition, expressed as a function that's aware of the HTTP response and
     *     the Session
     * @return the next DSL step
     */
    public TypedCondition checkIf(BiFunction<byte[], Session, Boolean> condition) {
      return new TypedCondition(ScalaWsFrameCheckBinaryConditions.typed(wrapped, condition));
    }

    public static final class TypedCondition {
      private final ScalaWsFrameCheckBinaryConditions.Typed wrapped;

      public TypedCondition(ScalaWsFrameCheckBinaryConditions.Typed wrapped) {
        this.wrapped = wrapped;
      }

      /**
       * Define the checks to apply when the condition holds true.
       *
       * @param thenChecks the checks
       * @return a new Binary instance
       */
      public Binary then(CheckBuilder... thenChecks) {
        return then(Arrays.asList(thenChecks));
      }

      /**
       * Define the checks to apply when the condition holds true.
       *
       * @param thenChecks the checks
       * @return a new Binary instance
       */
      public Binary then(List<CheckBuilder> thenChecks) {
        return wrapped.then_(thenChecks);
      }
    }

    /**
     * Make the check silent, not logged by the reporting engine
     *
     * @return a new Binary instance
     */
    public @NonNull Binary silent() {
      return new Binary(wrapped.silent());
    }

    @Override
    public io.gatling.http.check.ws.WsFrameCheck asScala() {
      return wrapped;
    }
  }

  /**
   * DSL for building WebSocket TEXT frames checks
   *
   * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
   */
  public static final class Text extends WsFrameCheck {
    private final io.gatling.http.check.ws.WsFrameCheck.Text wrapped;

    public Text(io.gatling.http.check.ws.WsFrameCheck.Text wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define conditions that have to hold true to match inbound messages and apply the checks on
     * them
     *
     * @param newMatchConditions the conditions to match
     * @return a new Text instance
     */
    public @NonNull Text matching(@NonNull CheckBuilder... newMatchConditions) {
      return matching(Arrays.asList(newMatchConditions));
    }

    /**
     * Define conditions that have to hold true to match inbound messages and apply the checks on
     * them
     *
     * @param newMatchConditions the conditions to match
     * @return a new Text instance
     */
    public @NonNull Text matching(@NonNull List<CheckBuilder> newMatchConditions) {
      return new Text(wrapped.matching(WsChecks.toScalaTextChecks(newMatchConditions)));
    }

    /**
     * Define the checks to apply on inbound messages
     *
     * @param checks the checks
     * @return a new Text instance
     */
    public @NonNull Text check(@NonNull CheckBuilder... checks) {
      return check(Arrays.asList(checks));
    }

    /**
     * Define the checks to apply on inbound messages
     *
     * @param checks the checks
     * @return a new Text instance
     */
    public @NonNull Text check(@NonNull List<CheckBuilder> checks) {
      return new Text(wrapped.check(WsChecks.toScalaTextChecks(checks)));
    }

    /**
     * Define the checks to apply on inbound messages when a condition holds true.
     *
     * @param condition a condition, expressed as a function
     * @return the next DSL step
     */
    public UntypedCondition checkIf(Function<Session, Boolean> condition) {
      return new UntypedCondition(ScalaWsFrameCheckTextConditions.untyped(wrapped, condition));
    }

    /**
     * Define the checks to apply on inbound messages when a condition holds true.
     *
     * @param condition a condition, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    public UntypedCondition checkIf(String condition) {
      return new UntypedCondition(ScalaWsFrameCheckTextConditions.untyped(wrapped, condition));
    }

    public static final class UntypedCondition {
      private final ScalaWsFrameCheckTextConditions.Untyped wrapped;

      public UntypedCondition(ScalaWsFrameCheckTextConditions.Untyped wrapped) {
        this.wrapped = wrapped;
      }

      /**
       * Define the checks to apply on inbound messages when a condition holds true.
       *
       * @param thenChecks the checks
       * @return a new Text instance
       */
      public Text then(CheckBuilder... thenChecks) {
        return then(Arrays.asList(thenChecks));
      }

      /**
       * Define the checks to apply when the condition holds true.
       *
       * @param thenChecks the checks
       * @return a new Text instance
       */
      public Text then(List<CheckBuilder> thenChecks) {
        return wrapped.then_(thenChecks);
      }
    }

    /**
     * Define the checks to apply on inbound messages when a condition holds true.
     *
     * @param condition a condition, expressed as a function that's aware of the HTTP response and
     *     the Session
     * @return the next DSL step
     */
    public TypedCondition checkIf(BiFunction<String, Session, Boolean> condition) {
      return new TypedCondition(ScalaWsFrameCheckTextConditions.typed(wrapped, condition));
    }

    public static final class TypedCondition {
      private final ScalaWsFrameCheckTextConditions.Typed wrapped;

      public TypedCondition(ScalaWsFrameCheckTextConditions.Typed wrapped) {
        this.wrapped = wrapped;
      }

      /**
       * Define the checks to apply when the condition holds true.
       *
       * @param thenChecks the checks
       * @return a new Text instance
       */
      public Text then(CheckBuilder... thenChecks) {
        return then(Arrays.asList(thenChecks));
      }

      /**
       * Define the checks to apply when the condition holds true.
       *
       * @param thenChecks the checks
       * @return a new Text instance
       */
      public Text then(List<CheckBuilder> thenChecks) {
        return wrapped.then_(thenChecks);
      }
    }

    /**
     * Make the check silent, not logged by the reporting engine
     *
     * @return a new Text instance
     */
    public @NonNull Text silent() {
      return new Text(wrapped.silent());
    }

    @Override
    public io.gatling.http.check.ws.WsFrameCheck asScala() {
      return wrapped;
    }
  }
}
