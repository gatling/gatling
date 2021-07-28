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
import io.gatling.http.javaapi.internal.SseChecks;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * DSL for building <a
 * href="https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events">SSE</a>
 * checks
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class SseMessageCheck {

  private final io.gatling.http.check.sse.SseMessageCheck wrapped;

  SseMessageCheck(io.gatling.http.check.sse.SseMessageCheck wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.http.check.sse.SseMessageCheck asScala() {
    return wrapped;
  }

  /**
   * Define conditions that have to hold true to match inbound messages and apply the checks on them
   *
   * @param newMatchConditions the conditions to match
   * @return a new SseMessageCheck instance
   */
  @Nonnull
  public SseMessageCheck matching(@Nonnull CheckBuilder... newMatchConditions) {
    return matching(Arrays.asList(newMatchConditions));
  }

  /**
   * Define conditions that have to hold true to match inbound messages and apply the checks on them
   *
   * @param newMatchConditions the conditions to match
   * @return a new SseMessageCheck instance
   */
  @Nonnull
  public SseMessageCheck matching(@Nonnull List<CheckBuilder> newMatchConditions) {
    return new SseMessageCheck(wrapped.matching(SseChecks.toScalaChecks(newMatchConditions)));
  }

  /**
   * Define the checks to apply on inbound messages
   *
   * @param checks the checks
   * @return a new SseMessageCheck instance
   */
  @Nonnull
  public SseMessageCheck check(@Nonnull CheckBuilder... checks) {
    return check(Arrays.asList(checks));
  }

  /**
   * Define the checks to apply on inbound messages
   *
   * @param checks the checks
   * @return a new SseMessageCheck instance
   */
  @Nonnull
  public SseMessageCheck check(@Nonnull List<CheckBuilder> checks) {
    return new SseMessageCheck(wrapped.check(SseChecks.toScalaChecks(checks)));
  }
}
