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

package io.gatling.core.javaapi;

import java.time.Duration;

import static io.gatling.core.javaapi.internal.ScalaHelpers.toScalaDuration;

public class OpenInjectionStep {

  private final io.gatling.core.controller.inject.open.OpenInjectionStep wrapped;

  OpenInjectionStep(io.gatling.core.controller.inject.open.OpenInjectionStep wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.core.controller.inject.open.OpenInjectionStep asScala() {
    return wrapped;
  }

  public static final class RampBuilder {
    private final int users;

    RampBuilder(int users) {
      this.users = users;
    }

    public OpenInjectionStep during(int durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    public OpenInjectionStep during(Duration duration) {
      return new OpenInjectionStep(new io.gatling.core.controller.inject.open.RampOpenInjection(users, toScalaDuration(duration)));
    }
  }

  public static final class HeavisideBuilder {
    private final int users;

    HeavisideBuilder(int users) {
      this.users = users;
    }

    public OpenInjectionStep during(int durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    public OpenInjectionStep during(Duration duration) {
      return new OpenInjectionStep(new io.gatling.core.controller.inject.open.HeavisideOpenInjection(users, toScalaDuration(duration)));
    }
  }

  public static final class ConstantRateBuilder {
    private final double rate;

    ConstantRateBuilder(double rate) {
      this.rate = rate;
    }

    public OpenInjectionStep during(int durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    public OpenInjectionStep during(Duration duration) {
      return new OpenInjectionStep(new io.gatling.core.controller.inject.open.ConstantRateOpenInjection(rate, toScalaDuration(duration)));
    }
  }

  public static final class PartialRampRateBuilder {
    private final double rate1;

    PartialRampRateBuilder(double rate1) {
      this.rate1 = rate1;
    }


    public RampRateBuilder to(double rate2) {
      return new RampRateBuilder(rate1, rate2);
    }
  }

  public static final class RampRateBuilder {
    private final double rate1;
    private final double rate2;

    RampRateBuilder(double rate1, double rate2) {
      this.rate1 = rate1;
      this.rate2 = rate2;
    }

    public OpenInjectionStep during(int durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    public OpenInjectionStep during(Duration duration) {
      return new OpenInjectionStep(new io.gatling.core.controller.inject.open.RampRateOpenInjection(rate1, rate2, toScalaDuration(duration)));
    }
  }

  public static final class IncreasingUsersPerSecProfileBuilder {
    private final double usersPerSec;

    IncreasingUsersPerSecProfileBuilder(double usersPerSec) {
      this.usersPerSec = usersPerSec;
    }

    public IncreasingUsersPerSecProfileBuilderWithTime times(int nbOfSteps) {
      return new IncreasingUsersPerSecProfileBuilderWithTime(usersPerSec, nbOfSteps);
    }
  }

  public static final class IncreasingUsersPerSecProfileBuilderWithTime {
    private final double usersPerSec;
    private final int nbOfSteps;

    IncreasingUsersPerSecProfileBuilderWithTime(double usersPerSec, int nbOfSteps) {
      this.usersPerSec = usersPerSec;
      this.nbOfSteps = nbOfSteps;
    }

    public IncreasingUsersPerSecCompositeStep eachLevelLasting(int durationSeconds) {
      return eachLevelLasting(Duration.ofSeconds(durationSeconds));
    }

    public IncreasingUsersPerSecCompositeStep eachLevelLasting(Duration duration) {
      return new IncreasingUsersPerSecCompositeStep(new io.gatling.core.controller.inject.open.IncreasingUsersPerSecProfileBuilderWithTime(usersPerSec, nbOfSteps).eachLevelLasting(toScalaDuration(duration)));
    }
  }

  public static final class IncreasingUsersPerSecCompositeStep extends OpenInjectionStep {
    IncreasingUsersPerSecCompositeStep(io.gatling.core.controller.inject.open.OpenInjectionStep wrapped) {
      super(wrapped);
    }

    public IncreasingUsersPerSecCompositeStep startingFrom(double startingUsers) {
      io.gatling.core.controller.inject.open.IncreasingUsersPerSecCompositeStep step = (io.gatling.core.controller.inject.open.IncreasingUsersPerSecCompositeStep) asScala();

      return new IncreasingUsersPerSecCompositeStep(
        new io.gatling.core.controller.inject.open.IncreasingUsersPerSecCompositeStep(
          step.usersPerSec(),
          step.nbOfSteps(),
          step.duration(),
          startingUsers,
          step.rampDuration()
        )
      );
    }

    public IncreasingUsersPerSecCompositeStep separatedByRampsLasting(int durationSeconds) {
      return separatedByRampsLasting(Duration.ofSeconds(durationSeconds));
    }

    public IncreasingUsersPerSecCompositeStep separatedByRampsLasting(Duration duration) {
      io.gatling.core.controller.inject.open.IncreasingUsersPerSecCompositeStep step = (io.gatling.core.controller.inject.open.IncreasingUsersPerSecCompositeStep) asScala();

      return new IncreasingUsersPerSecCompositeStep(
        new io.gatling.core.controller.inject.open.IncreasingUsersPerSecCompositeStep(
          step.usersPerSec(),
          step.nbOfSteps(),
          step.duration(),
          step.startingUsers(),
          toScalaDuration(duration)
        )
      );
    }
  }
}
