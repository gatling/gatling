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

public class ClosedInjectionStep {

  private final io.gatling.core.controller.inject.closed.ClosedInjectionStep wrapped;

  ClosedInjectionStep(io.gatling.core.controller.inject.closed.ClosedInjectionStep wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.core.controller.inject.closed.ClosedInjectionStep asScala() {
    return wrapped;
  }

  public static final class ConstantConcurrentUsersBuilder {
    private final int users;

    ConstantConcurrentUsersBuilder(int users) {
      this.users = users;
    }

    public ClosedInjectionStep during(int durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    public ClosedInjectionStep during(Duration duration) {
      return new ClosedInjectionStep(new io.gatling.core.controller.inject.closed.ConstantConcurrentUsersInjection(users, toScalaDuration(duration)));
    }
  }

  public static final class RampConcurrentUsersInjectionFrom {
    private final int from;

    public RampConcurrentUsersInjectionFrom(int from) {
      this.from = from;
    }

    public RampConcurrentUsersInjectionTo to(int t) {
      return new RampConcurrentUsersInjectionTo(from, t);
    }
  }

  public static final class RampConcurrentUsersInjectionTo {
    private final int from;
    private final int to;

    public RampConcurrentUsersInjectionTo(int from, int to) {
      this.from = from;
      this.to = to;
    }

    public ClosedInjectionStep during(int durationSeconds) {
      return during(Duration.ofSeconds(durationSeconds));
    }

    public ClosedInjectionStep during(Duration duration) {
      return new ClosedInjectionStep(new io.gatling.core.controller.inject.closed.RampConcurrentUsersInjection(from, to, toScalaDuration(duration)));
    }
  }

  public static final class IncreasingConcurrentUsersProfileBuilder {
    private final int concurrentUsers;

    public IncreasingConcurrentUsersProfileBuilder(int concurrentUsers) {
      this.concurrentUsers = concurrentUsers;
    }

    public IncreasingConcurrentUsersProfileBuilderWithTime times(int nbOfSteps) {
      return new IncreasingConcurrentUsersProfileBuilderWithTime(concurrentUsers, nbOfSteps);
    }
  }

  public static final class IncreasingConcurrentUsersProfileBuilderWithTime {
    private final int concurrentUsers;
    private final int nbOfSteps;

    public IncreasingConcurrentUsersProfileBuilderWithTime(int concurrentUsers, int nbOfSteps) {
      this.concurrentUsers = concurrentUsers;
      this.nbOfSteps = nbOfSteps;
    }

    public IncreasingConcurrentUsersCompositeStep eachLevelLasting(int durationSeconds) {
      return eachLevelLasting(Duration.ofSeconds(durationSeconds));
    }

    public IncreasingConcurrentUsersCompositeStep eachLevelLasting(Duration duration) {
      return new IncreasingConcurrentUsersCompositeStep(new io.gatling.core.controller.inject.closed.IncreasingConcurrentUsersProfileBuilderWithTime(concurrentUsers, nbOfSteps).eachLevelLasting(toScalaDuration(duration)));
    }
  }

  public static final class IncreasingConcurrentUsersCompositeStep extends ClosedInjectionStep {
    IncreasingConcurrentUsersCompositeStep(io.gatling.core.controller.inject.closed.ClosedInjectionStep wrapped) {
      super(wrapped);
    }

    public IncreasingConcurrentUsersCompositeStep startingFrom(int startingUsers) {
      io.gatling.core.controller.inject.closed.IncreasingConcurrentUsersCompositeStep step = (io.gatling.core.controller.inject.closed.IncreasingConcurrentUsersCompositeStep) asScala();

      return new IncreasingConcurrentUsersCompositeStep(
        new io.gatling.core.controller.inject.closed.IncreasingConcurrentUsersCompositeStep(
          step.concurrentUsers(),
          step.nbOfSteps(),
          step.duration(),
          startingUsers,
          step.rampDuration()
        )
      );
    }

    public IncreasingConcurrentUsersCompositeStep separatedByRampsLasting(int durationSeconds) {
      return separatedByRampsLasting(Duration.ofSeconds(durationSeconds));
    }

    public IncreasingConcurrentUsersCompositeStep separatedByRampsLasting(Duration duration) {
      io.gatling.core.controller.inject.closed.IncreasingConcurrentUsersCompositeStep step = (io.gatling.core.controller.inject.closed.IncreasingConcurrentUsersCompositeStep) asScala();

      return new IncreasingConcurrentUsersCompositeStep(
        new io.gatling.core.controller.inject.closed.IncreasingConcurrentUsersCompositeStep(
          step.concurrentUsers(),
          step.nbOfSteps(),
          step.duration(),
          step.startingUsers(),
          toScalaDuration(duration)
        )
      );
    }
  }
}
