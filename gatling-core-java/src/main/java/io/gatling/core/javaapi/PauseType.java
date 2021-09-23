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

import io.gatling.commons.validation.Validation;
import scala.Function1;

import java.time.Duration;

import static io.gatling.core.javaapi.internal.ScalaHelpers.toScalaDuration;

public abstract class PauseType {

  private PauseType() {
  }

  public abstract io.gatling.core.pause.PauseType asScala();

  public static final PauseType Disabled = new PauseType() {
    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return io.gatling.core.pause.Disabled$.MODULE$;
    }
  };

  public static final PauseType Constant = new PauseType() {
    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return io.gatling.core.pause.Constant$.MODULE$;
    }
  };

  public static final PauseType Exponential = new PauseType() {
    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return io.gatling.core.pause.Exponential$.MODULE$;
    }
  };

  public static final class NormalWithPercentageDuration extends PauseType {
    private final double stdDev;

    public NormalWithPercentageDuration(double stdDev) {
      this.stdDev = stdDev;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.NormalWithPercentageDuration(stdDev);
    }
  }

  public static final class NormalWithStdDevDuration extends PauseType {
    private final Duration stdDev;

    public NormalWithStdDevDuration(Duration stdDev) {
      this.stdDev = stdDev;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.NormalWithStdDevDuration(toScalaDuration(stdDev));
    }
  }

  public static final class Custom extends PauseType {
    private final Function1<io.gatling.core.session.Session, Validation<Object>> custom;

    public Custom(Function1<io.gatling.core.session.Session, Validation<Object>> custom) {
      this.custom = custom;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.Custom(custom);
    }
  }

  public static final class UniformPercentage extends PauseType {
    private final double plusOrMinus;

    public UniformPercentage(double plusOrMinus) {
      this.plusOrMinus = plusOrMinus;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.UniformPercentage(plusOrMinus);
    }
  }

  public static final class UniformDuration extends PauseType {
    private final Duration plusOrMinus;

    public UniformDuration(Duration plusOrMinus) {
      this.plusOrMinus = plusOrMinus;
    }

    @Override
    public io.gatling.core.pause.PauseType asScala() {
      return new io.gatling.core.pause.UniformDuration(toScalaDuration(plusOrMinus));
    }
  }
}
