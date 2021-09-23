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

public final class ThrottleStep {

  private final io.gatling.core.controller.throttle.ThrottleStep wrapped;

  public ThrottleStep(io.gatling.core.controller.throttle.ThrottleStep wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.core.controller.throttle.ThrottleStep asScala() {
    return wrapped;
  }

  public static final class ReachIntermediate {
    private final int target;

    public ReachIntermediate(int target) {
      this.target = target;
    }

    public ThrottleStep in(int duration) {
      return in(Duration.ofSeconds(duration));
    }

    public ThrottleStep in(Duration duration) {
      return new ThrottleStep(new io.gatling.core.controller.throttle.Reach(target, toScalaDuration(duration)));
    }
  }
}
