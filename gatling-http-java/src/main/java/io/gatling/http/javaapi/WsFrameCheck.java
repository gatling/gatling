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

package io.gatling.http.javaapi;

import io.gatling.core.javaapi.CheckBuilder;
import io.gatling.http.javaapi.internal.WsChecks;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

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

    Binary(io.gatling.http.check.ws.WsFrameCheck.Binary wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define conditions that have to hold true to match inbound messages and apply the checks on
     * them
     *
     * @param newMatchConditions the conditions to match
     * @return a new Binary instance
     */
    @Nonnull
    public Binary matching(@Nonnull CheckBuilder... newMatchConditions) {
      return matching(Arrays.asList(newMatchConditions));
    }

    /**
     * Define conditions that have to hold true to match inbound messages and apply the checks on
     * them
     *
     * @param newMatchConditions the conditions to match
     * @return a new Binary instance
     */
    @Nonnull
    public Binary matching(@Nonnull List<CheckBuilder> newMatchConditions) {
      return new Binary(wrapped.matching(WsChecks.toScalaBinaryChecks(newMatchConditions)));
    }

    /**
     * Define the checks to apply on inbound messages
     *
     * @param checks the checks
     * @return a new Binary instance
     */
    @Nonnull
    public Binary check(@Nonnull CheckBuilder... checks) {
      return check(Arrays.asList(checks));
    }

    /**
     * Define the checks to apply on inbound messages
     *
     * @param checks the checks
     * @return a new Binary instance
     */
    @Nonnull
    public Binary check(@Nonnull List<CheckBuilder> checks) {
      return new Binary(wrapped.check(WsChecks.toScalaBinaryChecks(checks)));
    }

    /**
     * Make the check silent, not logged by the reporting engine
     *
     * @return a new Binary instance
     */
    @Nonnull
    public Binary silent() {
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

    Text(io.gatling.http.check.ws.WsFrameCheck.Text wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define conditions that have to hold true to match inbound messages and apply the checks on
     * them
     *
     * @param newMatchConditions the conditions to match
     * @return a new Text instance
     */
    @Nonnull
    public Text matching(@Nonnull CheckBuilder... newMatchConditions) {
      return matching(Arrays.asList(newMatchConditions));
    }

    /**
     * Define conditions that have to hold true to match inbound messages and apply the checks on
     * them
     *
     * @param newMatchConditions the conditions to match
     * @return a new Text instance
     */
    @Nonnull
    public Text matching(@Nonnull List<CheckBuilder> newMatchConditions) {
      return new Text(wrapped.matching(WsChecks.toScalaTextChecks(newMatchConditions)));
    }

    /**
     * Define the checks to apply on inbound messages
     *
     * @param checks the checks
     * @return a new Text instance
     */
    @Nonnull
    public Text check(@Nonnull CheckBuilder... checks) {
      return check(Arrays.asList(checks));
    }

    /**
     * Define the checks to apply on inbound messages
     *
     * @param checks the checks
     * @return a new Text instance
     */
    @Nonnull
    public Text check(@Nonnull List<CheckBuilder> checks) {
      return new Text(wrapped.check(WsChecks.toScalaTextChecks(checks)));
    }

    /**
     * Make the check silent, not logged by the reporting engine
     *
     * @return a new Text instance
     */
    @Nonnull
    public Text silent() {
      return new Text(wrapped.silent());
    }

    @Override
    public io.gatling.http.check.ws.WsFrameCheck asScala() {
      return wrapped;
    }
  }
}
