/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import io.gatling.javaapi.core.internal.ClosedInjectionSteps;
import java.time.Duration;
import javax.annotation.Nonnull;

/**
 * An injection profile step for using a closed workload model where you control the concurrent
 * number of users. Only use if your system has a queue limiting entry. Don't use otherwise or your
 * test will not match any production use case.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public class ClosedInjectionStep {

  private final io.gatling.core.controller.inject.closed.ClosedInjectionStep wrapped;

  private ClosedInjectionStep(
      @Nonnull io.gatling.core.controller.inject.closed.ClosedInjectionStep wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  @Nonnull
  public io.gatling.core.controller.inject.closed.ClosedInjectionStep asScala() {
    return wrapped;
  }

  /**
   * DSL component for building a {@link ClosedInjectionStep} that will inject new users in a way to
   * maintain a constant number of concurrent users for a given duration.
   */
  public static final class Constant {
    private final int users;

    Constant(int users) {
      this.users = users;
    }

    /**
     * Define the duration of the step
     *
     * @param durationSeconds the duration in seconds
     * @return a new ClosedInjectionStep
     */
    @Nonnull
    public ClosedInjectionStep during(long durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    /**
     * Define the duration of the step
     *
     * @param duration the duration
     * @return a new ClosedInjectionStep
     */
    @Nonnull
    public ClosedInjectionStep during(@Nonnull Duration duration) {
      return new ClosedInjectionStep(
          new io.gatling.core.controller.inject.closed.ConstantConcurrentUsersInjection(
              users, toScalaDuration(duration)));
    }
  }

  /**
   * DSL step for building a {@link ClosedInjectionStep} that will inject new users in a way to ramp
   * the number of concurrent users for a given duration.
   */
  public static final class Ramp {
    private final int from;

    public Ramp(int from) {
      this.from = from;
    }

    /**
     * Define the target number of concurrent users at the end of the ramp.
     *
     * @param t the target number
     * @return a RampConcurrentUsersInjectionTo
     */
    @Nonnull
    public RampTo to(int t) {
      return new RampTo(from, t);
    }
  }

  /**
   * DSL step for building a {@link ClosedInjectionStep} that will inject new users in a way to ramp
   * the number of concurrent users for a given duration.
   */
  public static final class RampTo {
    private final int from;
    private final int to;

    public RampTo(int from, int to) {
      this.from = from;
      this.to = to;
    }

    /**
     * Define the duration of the ramp.
     *
     * @param durationSeconds the duration in seconds
     * @return a complete ClosedInjectionStep
     */
    @Nonnull
    public ClosedInjectionStep during(long durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    /**
     * Define the duration of the ramp.
     *
     * @param duration the duration
     * @return a complete ClosedInjectionStep
     */
    @Nonnull
    public ClosedInjectionStep during(@Nonnull Duration duration) {
      return new ClosedInjectionStep(
          new io.gatling.core.controller.inject.closed.RampConcurrentUsersInjection(
              from, to, toScalaDuration(duration)));
    }
  }

  /**
   * DSL step for building a {@link ClosedInjectionStep} that will inject new users in a way to ramp
   * the number of concurrent users in a stairs fashion
   */
  public static final class Stairs {
    private final int usersIncrement;

    Stairs(int usersIncrement) {
      this.usersIncrement = usersIncrement;
    }

    /**
     * Define the number of levels
     *
     * @param levels the number of levels in the stairs
     * @return the next DSL step
     */
    @Nonnull
    public StairsWithTime times(int levels) {
      return new StairsWithTime(usersIncrement, levels);
    }
  }

  /**
   * DSL step for building a {@link ClosedInjectionStep} that will inject new users in a way to ramp
   * the number of concurrent users in a stairs fashion
   */
  public static final class StairsWithTime {
    private final int usersIncrement;
    private final int levels;

    public StairsWithTime(int usersIncrement, int levels) {
      this.usersIncrement = usersIncrement;
      this.levels = levels;
    }

    /**
     * Define the duration of each level
     *
     * @param durationSeconds the duration in seconds
     * @return the next DSL step
     */
    @Nonnull
    public Composite eachLevelLasting(long durationSeconds) {
      return eachLevelLasting(Duration.ofSeconds(durationSeconds));
    }

    /**
     * Define the duration of each level
     *
     * @param duration the duration
     * @return the next DSL step
     */
    @Nonnull
    public Composite eachLevelLasting(@Nonnull Duration duration) {
      return new Composite(
          ClosedInjectionSteps.newEachLevelLasting(usersIncrement, levels)
              .eachLevelLasting(toScalaDuration(duration)));
    }
  }

  /**
   * DSL step for building a {@link ClosedInjectionStep} that will inject new users in a way to ramp
   * the number of concurrent users in a stairs fashion
   */
  public static final class Composite extends ClosedInjectionStep {
    Composite(io.gatling.core.controller.inject.closed.ClosedInjectionStep wrapped) {
      super(wrapped);
    }

    /**
     * Define the initial number of concurrent users (optional)
     *
     * @param startingUsers the initial number of concurrent users
     * @return a usable {@link ClosedInjectionStep}
     */
    @Nonnull
    public Composite startingFrom(int startingUsers) {
      io.gatling.core.controller.inject.closed.StairsConcurrentUsersCompositeStep step =
          (io.gatling.core.controller.inject.closed.StairsConcurrentUsersCompositeStep) asScala();

      return new Composite(
          new io.gatling.core.controller.inject.closed.StairsConcurrentUsersCompositeStep(
              step.usersIncrement(),
              step.levels(),
              step.duration(),
              startingUsers,
              step.rampDuration()));
    }

    /**
     * Define ramps separating levels (optional)
     *
     * @param durationSeconds the duration of the ramps in seconds
     * @return a usable {@link ClosedInjectionStep}
     */
    @Nonnull
    public Composite separatedByRampsLasting(long durationSeconds) {
      return separatedByRampsLasting(Duration.ofSeconds(durationSeconds));
    }

    /**
     * Define ramps separating levels (optional)
     *
     * @param duration the duration of the ramps
     * @return a usable {@link ClosedInjectionStep}
     */
    @Nonnull
    public Composite separatedByRampsLasting(Duration duration) {
      io.gatling.core.controller.inject.closed.StairsConcurrentUsersCompositeStep step =
          (io.gatling.core.controller.inject.closed.StairsConcurrentUsersCompositeStep) asScala();

      return new Composite(
          new io.gatling.core.controller.inject.closed.StairsConcurrentUsersCompositeStep(
              step.usersIncrement(),
              step.levels(),
              step.duration(),
              step.startingUsers(),
              toScalaDuration(duration)));
    }
  }
}
