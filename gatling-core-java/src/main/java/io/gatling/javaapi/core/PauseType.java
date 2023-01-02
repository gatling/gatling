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
import static io.gatling.javaapi.core.internal.Expressions.javaLongFunctionToExpression;

import java.time.Duration;
import java.util.function.Function;
import javax.annotation.Nonnull;

/** The type of pauses to use on a given Scenario or Simulation. */
public abstract class PauseType {

  private PauseType() {}

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  public abstract io.gatling.core.pause.PauseType asScala();

  /** Pauses are disabled. Note: this type is forced when using Throttling. */
  public static final PauseType Disabled =
      new PauseType() {
        @Override
        public io.gatling.core.pause.PauseType asScala() {
          return io.gatling.core.pause.Disabled$.MODULE$;
        }
      };

  /** Pauses use the values defined in the Scenario. */
  public static final PauseType Constant =
      new PauseType() {
        @Override
        public io.gatling.core.pause.PauseType asScala() {
          return io.gatling.core.pause.Constant$.MODULE$;
        }
      };

  /**
   * Pauses use an <a href="https://en.wikipedia.org/wiki/Exponential_distribution">exponential
   * distribution</a> whose mean is the value defined in the Scenario.
   */
  public static final PauseType Exponential =
      new PauseType() {
        @Override
        public io.gatling.core.pause.PauseType asScala() {
          return io.gatling.core.pause.Exponential$.MODULE$;
        }
      };

  /**
   * Pauses use a <a href="https://en.wikipedia.org/wiki/Normal_distribution">normal
   * distribution</a> where the standard deviation is defined as a percentage of the value defined
   * in the Scenario.
   */
  public static final class NormalWithPercentageDuration extends PauseType {
    private final double stdDev;

    /**
     * Create a normal with a standard deviation is defined as a percentage of the value defined in
     * the Scenario.
     *
     * @param stdDev the standard deviation of the distribution in percents.
     */
    NormalWithPercentageDuration(double stdDev) {
      this.stdDev = stdDev;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.NormalWithPercentageDuration(stdDev);
    }
  }

  /**
   * Pauses use a <a href="https://en.wikipedia.org/wiki/Normal_distribution">normal
   * distribution</a> where the standard deviation is defined as an absolute value.
   */
  public static final class NormalWithStdDevDuration extends PauseType {
    private final Duration stdDev;

    /**
     * Create a normal with a standard deviation is defined as an absolute value.
     *
     * @param stdDev the standard deviation of the distribution
     */
    NormalWithStdDevDuration(@Nonnull Duration stdDev) {
      this.stdDev = stdDev;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.NormalWithStdDevDuration(toScalaDuration(stdDev));
    }
  }

  /** Pauses use a custom strategy based on a user provided function */
  public static final class Custom extends PauseType {
    private final Function<Session, Long> f;

    Custom(@Nonnull Function<Session, Long> f) {
      this.f = f;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.Custom(javaLongFunctionToExpression(f));
    }
  }

  /**
   * Pauses are distributed uniformly in a range around the mean value defined in the Scenario.
   * Half-width is expressed as a percentage of the mean.
   */
  public static final class UniformPercentage extends PauseType {
    private final double plusOrMinus;

    UniformPercentage(double plusOrMinus) {
      this.plusOrMinus = plusOrMinus;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.UniformPercentage(plusOrMinus);
    }
  }

  /**
   * Pauses are distributed uniformly in a range around the mean value defined in the Scenario.
   * Half-width is expressed as an absolute value.
   */
  public static final class UniformDuration extends PauseType {
    private final Duration plusOrMinus;

    public UniformDuration(@Nonnull Duration plusOrMinus) {
      this.plusOrMinus = plusOrMinus;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.UniformDuration(toScalaDuration(plusOrMinus));
    }
  }
}
