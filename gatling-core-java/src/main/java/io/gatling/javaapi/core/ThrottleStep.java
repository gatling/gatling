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

package io.gatling.javaapi.core;

import static io.gatling.javaapi.core.internal.Converters.toScalaDuration;

import java.time.Duration;
import javax.annotation.Nonnull;

/**
 * Java wrapper of a Scala ThrottleStep.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class ThrottleStep {

  private final io.gatling.core.controller.throttle.ThrottleStep wrapped;

  ThrottleStep(io.gatling.core.controller.throttle.ThrottleStep wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.core.controller.throttle.ThrottleStep asScala() {
    return wrapped;
  }

  /** DSL step to define the duration of a throttling ramp. */
  public static final class ReachIntermediate {
    private final int target;

    ReachIntermediate(int target) {
      this.target = target;
    }

    /**
     * Define the duration of a throttling ramp
     *
     * @param duration the duration in seconds
     * @return a new ThrottleStep
     */
    @Nonnull
    public ThrottleStep in(long duration) {
      return in(Duration.ofSeconds(duration));
    }

    /**
     * Alias for `in` that's a reserved keyword in Kotlin
     *
     * @param duration the duration in seconds
     * @return a new ThrottleStep
     */
    @Nonnull
    public ThrottleStep during(long duration) {
      return in(duration);
    }

    /**
     * Define the duration of a throttling ramp
     *
     * @param duration the duration
     * @return a new ThrottleStep
     */
    @Nonnull
    public ThrottleStep in(@Nonnull Duration duration) {
      return new ThrottleStep(
          new io.gatling.core.controller.throttle.Reach(target, toScalaDuration(duration)));
    }

    /**
     * Alias for `in` that's a reserved keyword in Kotlin
     *
     * @param duration the duration
     * @return a new ThrottleStep
     */
    @Nonnull
    public ThrottleStep during(@Nonnull Duration duration) {
      return in(duration);
    }
  }
}
