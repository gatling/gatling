/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.internal.OpenInjectionSteps;
import java.time.Duration;

/**
 * An injection profile step for using an open workload model where you control the arrival rate of
 * new users. In 99.99% of the cases, the right choice, over closed workload model.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public class OpenInjectionStep {

  private final io.gatling.core.controller.inject.open.OpenInjectionStep wrapped;

  private OpenInjectionStep(
      @NonNull io.gatling.core.controller.inject.open.OpenInjectionStep wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Inject a bunch of users at the same time.
   *
   * @param users the number of users to inject
   * @return a new OpenInjectionStep
   */
  @NonNull
  public static OpenInjectionStep atOnceUsers(int users) {
    return new OpenInjectionStep(
        new io.gatling.core.controller.inject.open.AtOnceOpenInjection(users));
  }

  /**
   * Don't inject any new user for a given duration
   *
   * @param duration the duration
   * @return a new OpenInjectionStep
   */
  @NonNull
  public static OpenInjectionStep nothingFor(@NonNull Duration duration) {
    return new OpenInjectionStep(
        new io.gatling.core.controller.inject.open.NothingForOpenInjection(
            toScalaDuration(duration)));
  }

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  @NonNull
  public io.gatling.core.controller.inject.open.OpenInjectionStep asScala() {
    return wrapped;
  }

  /**
   * A DSL for creating a {@link OpenInjectionStep} that will inject a stock of users distributed
   * evenly on a given period of time. Strictly equivalent to {@link ConstantRate}
   */
  public static final class Ramp {
    private final io.gatling.core.controller.inject.open.OpenInjectionBuilder.Ramp wrapped;

    Ramp(int users) {
      this.wrapped = new io.gatling.core.controller.inject.open.OpenInjectionBuilder.Ramp(users);
    }

    /**
     * Define the duration of the ramp
     *
     * @param durationSeconds the ramp duration in seconds
     * @return a new OpenInjectionStep
     */
    @NonNull
    public OpenInjectionStep during(long durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    /**
     * Define the duration of the ramp
     *
     * @param duration the ramp duration
     * @return a new OpenInjectionStep
     */
    @NonNull
    public OpenInjectionStep during(@NonNull Duration duration) {
      return new OpenInjectionStep(wrapped.during(toScalaDuration(duration)));
    }
  }

  /**
   * A DSL for creating a {@link OpenInjectionStep} that will inject a stock of users distributed
   * with a <a hreh="https://en.wikipedia.org/wiki/Heaviside_step_function">Heaviside</a>
   * distribution on a given period of time. Strictly equivalent to {@link ConstantRate}
   */
  public static final class StressPeak {
    private final io.gatling.core.controller.inject.open.OpenInjectionBuilder.StressPeak wrapped;

    StressPeak(int users) {
      this.wrapped =
          new io.gatling.core.controller.inject.open.OpenInjectionBuilder.StressPeak(users);
    }

    /**
     * Define the duration of the Heaviside distribution
     *
     * @param durationSeconds the duration in seconds
     * @return a new OpenInjectionStep
     */
    @NonNull
    public OpenInjectionStep during(long durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    /**
     * Define the duration of the Heaviside distribution
     *
     * @param duration the duration
     * @return a new OpenInjectionStep
     */
    @NonNull
    public OpenInjectionStep during(@NonNull Duration duration) {
      return new OpenInjectionStep(wrapped.during(toScalaDuration(duration)));
    }
  }

  /**
   * A DSL for creating a {@link OpenInjectionStep} that will inject users at a constant rate for a
   * given duration.
   */
  public static final class ConstantRate {
    private final io.gatling.core.controller.inject.open.OpenInjectionBuilder.ConstantRate wrapped;

    ConstantRate(double rate) {
      this.wrapped =
          new io.gatling.core.controller.inject.open.OpenInjectionBuilder.ConstantRate(rate);
    }

    /**
     * Define the duration of the step
     *
     * @param durationSeconds the duration in seconds
     * @return a new OpenInjectionStep
     */
    @NonNull
    public ConstantRateOpenInjectionStep during(long durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    /**
     * Define the duration of the step
     *
     * @param duration the duration
     * @return a new OpenInjectionStep
     */
    @NonNull
    public ConstantRateOpenInjectionStep during(@NonNull Duration duration) {
      return new ConstantRateOpenInjectionStep(wrapped.during(toScalaDuration(duration)));
    }

    /** A special {@link OpenInjectionStep} that supports "randomized". */
    public static final class ConstantRateOpenInjectionStep extends OpenInjectionStep {

      private ConstantRateOpenInjectionStep(
          @NonNull io.gatling.core.controller.inject.open.ConstantRateOpenInjection wrapped) {
        super(wrapped);
      }

      public OpenInjectionStep randomized() {
        return new OpenInjectionStep(
            ((io.gatling.core.controller.inject.open.ConstantRateOpenInjection) asScala())
                .randomized());
      }
    }
  }

  /**
   * A DSL for creating a {@link OpenInjectionStep} that will inject users at a rate that will
   * increase linearly for a given duration.
   */
  public static final class RampRate {
    private final double from;

    RampRate(double from) {
      this.from = from;
    }

    /**
     * Define the target rate at the end of the ramp
     *
     * @param to the target rate
     * @return the next DSL step
     */
    @NonNull
    public During to(double to) {
      return new During(from, to);
    }

    /**
     * A DSL for creating a {@link OpenInjectionStep} that will inject users at a rate that will
     * increase linearly for a given duration.
     */
    public static final class During {
      private final double from;
      private final double to;

      private During(double from, double to) {
        this.from = from;
        this.to = to;
      }

      /**
       * Define the duration of the ramp
       *
       * @param durationSeconds the duration in seconds
       * @return a new OpenInjectionStep
       */
      @NonNull
      public RampRateOpenInjectionStep during(long durationSeconds) {
        return during(Duration.ofSeconds(durationSeconds));
      }

      /**
       * Define the duration of the ramp
       *
       * @param duration the duration
       * @return a new OpenInjectionStep
       */
      @NonNull
      public RampRateOpenInjectionStep during(@NonNull Duration duration) {
        return new RampRateOpenInjectionStep(
            OpenInjectionSteps.newRampRateTo(from, to).during(toScalaDuration(duration)));
      }
    }

    /** A special {@link OpenInjectionStep} that supports "randomized". */
    public static final class RampRateOpenInjectionStep extends OpenInjectionStep {

      private RampRateOpenInjectionStep(
          @NonNull io.gatling.core.controller.inject.open.RampRateOpenInjection wrapped) {
        super(wrapped);
      }

      public OpenInjectionStep randomized() {
        return new OpenInjectionStep(
            ((io.gatling.core.controller.inject.open.RampRateOpenInjection) asScala())
                .randomized());
      }
    }
  }

  /** A DSL for creating a {@link OpenInjectionStep} that will inject users with stairs rates. */
  public static final class Stairs {
    private final double rateIncrement;

    Stairs(double rateIncrement) {
      this.rateIncrement = rateIncrement;
    }

    /**
     * Define the number of levels
     *
     * @param levels the number of levels in the stairs
     * @return the next DSL step
     */
    @NonNull
    public Times times(int levels) {
      return new Times(rateIncrement, levels);
    }

    /** A DSL for creating a {@link OpenInjectionStep} that will inject users with stairs rates. */
    public static final class Times {
      private final double rateIncrement;
      private final int levels;

      private Times(double rateIncrement, int levels) {
        this.rateIncrement = rateIncrement;
        this.levels = levels;
      }

      /**
       * Define the duration of each level
       *
       * @param durationSeconds the duration in seconds
       * @return the next DSL step
       */
      @NonNull
      public Composite eachLevelLasting(long durationSeconds) {
        return eachLevelLasting(Duration.ofSeconds(durationSeconds));
      }

      /**
       * Define the duration of each level
       *
       * @param duration the duration
       * @return the next DSL step
       */
      @NonNull
      public Composite eachLevelLasting(@NonNull Duration duration) {
        return new Composite(
            OpenInjectionSteps.newEachLevelLasting(rateIncrement, levels)
                .eachLevelLasting(toScalaDuration(duration)));
      }
    }

    /** A DSL for creating a {@link OpenInjectionStep} that will inject users with stairs rates. */
    public static final class Composite extends OpenInjectionStep {
      Composite(io.gatling.core.controller.inject.open.StairsUsersPerSecCompositeStep wrapped) {
        super(wrapped);
      }

      private io.gatling.core.controller.inject.open.StairsUsersPerSecCompositeStep wrapped() {
        return (io.gatling.core.controller.inject.open.StairsUsersPerSecCompositeStep) asScala();
      }

      /**
       * Define the initial number of users per second rate (optional)
       *
       * @param startingRate the initial rate
       * @return a usable {@link OpenInjectionStep}
       */
      @NonNull
      public Composite startingFrom(double startingRate) {
        return new Composite(wrapped().startingFrom(startingRate));
      }

      /**
       * Define ramps separating levels (optional)
       *
       * @param durationSeconds the duration of the ramps in seconds
       * @return a usable {@link OpenInjectionStep}
       */
      @NonNull
      public Composite separatedByRampsLasting(long durationSeconds) {
        return separatedByRampsLasting(Duration.ofSeconds(durationSeconds));
      }

      /**
       * Define ramps separating levels (optional)
       *
       * @param duration the duration
       * @return a usable {@link OpenInjectionStep}
       */
      @NonNull
      public Composite separatedByRampsLasting(@NonNull Duration duration) {
        return new Composite(wrapped().separatedByRampsLasting(toScalaDuration(duration)));
      }
    }
  }
}
