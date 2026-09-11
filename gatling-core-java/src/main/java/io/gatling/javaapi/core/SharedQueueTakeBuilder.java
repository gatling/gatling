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

import static io.gatling.javaapi.core.internal.Converters.*;

import java.time.Duration;
import org.jspecify.annotations.NonNull;

/**
 * Builder of an action that pops the oldest value of a queue into the virtual user's Session,
 * waiting for one to be available if the queue is empty.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class SharedQueueTakeBuilder implements ActionBuilder {

  private final io.gatling.core.action.builder.SharedQueueTakeBuilder wrapped;

  SharedQueueTakeBuilder(io.gatling.core.action.builder.SharedQueueTakeBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Give up after the given duration instead of waiting forever. The virtual user is then marked as
   * failed and moves on, and the Session attribute is left untouched.
   *
   * <p>Timeouts are only meant to avoid stalling virtual users forever, so they're best effort:
   * expired virtual users are released periodically, hence they can wait up to one extra second.
   *
   * @param timeout the maximum duration the virtual user waits for a value
   * @return a new SharedQueueTakeBuilder
   */
  public @NonNull SharedQueueTakeBuilder timeout(@NonNull Duration timeout) {
    return new SharedQueueTakeBuilder(wrapped.timeout(toScalaDuration(timeout)));
  }

  /**
   * Give up after the given number of seconds instead of waiting forever. The virtual user is then
   * marked as failed and moves on, and the Session attribute is left untouched.
   *
   * <p>Timeouts are only meant to avoid stalling virtual users forever, so they're best effort:
   * expired virtual users are released periodically, hence they can wait up to one extra second.
   *
   * @param timeout the maximum number of seconds the virtual user waits for a value
   * @return a new SharedQueueTakeBuilder
   */
  public @NonNull SharedQueueTakeBuilder timeout(int timeout) {
    return timeout(Duration.ofSeconds(timeout));
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped;
  }
}
