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

import static io.gatling.core.javaapi.internal.Converters.*;
import static io.gatling.core.javaapi.internal.Expressions.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * A builder for a Population = a Scenario + an injection profile.
 *
 * <p>Immutable, meaning each method doesn't mutate the current instance but return a new one.
 */
public final class PopulationBuilder {
  private final io.gatling.core.structure.PopulationBuilder wrapped;

  public PopulationBuilder(io.gatling.core.structure.PopulationBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  public io.gatling.core.structure.PopulationBuilder asScala() {
    return wrapped;
  }

  /**
   * Define the optional protocols for this PopulationBuilder
   *
   * @param protocols the protocols
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder protocols(@Nonnull ProtocolBuilder... protocols) {
    return protocols(Arrays.asList(protocols));
  }

  /**
   * Define the optional protocols for this PopulationBuilder
   *
   * @param protocols the protocols
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder protocols(@Nonnull List<ProtocolBuilder> protocols) {
    return new PopulationBuilder(
        wrapped.protocols(
            toScalaSeq(
                protocols.stream().map(ProtocolBuilder::protocol).collect(Collectors.toList()))));
  }

  /**
   * Define some other PopulationBuilder to be executed once all the users of this PopulationBuilder
   * complete their Scenario.
   *
   * @param children the children PopulationBuilder
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder andThen(@Nonnull PopulationBuilder... children) {
    return andThen(Arrays.asList(children));
  }

  /**
   * Define some other PopulationBuilder to be executed once all the users of this PopulationBuilder
   * complete their Scenario.
   *
   * @param children the children PopulationBuilder
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder andThen(@Nonnull List<PopulationBuilder> children) {
    return new PopulationBuilder(
        wrapped.andThen(
            toScalaSeq(
                children.stream().map(PopulationBuilder::asScala).collect(Collectors.toList()))));
  }

  /**
   * Disable the pauses, see {@link PauseType#Disabled}
   *
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder disablePauses() {
    return new PopulationBuilder(wrapped.disablePauses());
  }

  /**
   * Use constant pauses, see {@link PauseType#Constant}
   *
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder constantPauses() {
    return new PopulationBuilder(wrapped.constantPauses());
  }

  /**
   * Use exponential pauses, see {@link PauseType#Exponential}
   *
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder exponentialPauses() {
    return new PopulationBuilder(wrapped.exponentialPauses());
  }

  /**
   * Use custom pauses, see {@link PauseType.Custom}
   *
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder customPauses(@Nonnull Function<Session, Long> custom) {
    return new PopulationBuilder(wrapped.customPauses(javaLongFunctionToExpression(custom)));
  }

  /**
   * Use uniform pauses with a standard deviation percentage, see {@link
   * PauseType.UniformPercentage}
   *
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder uniformPauses(double plusOrMinus) {
    return new PopulationBuilder(wrapped.uniformPauses(plusOrMinus));
  }

  /**
   * Use uniform pauses with a standard deviation value, see {@link PauseType.UniformDuration}
   *
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder uniformPauses(@Nonnull Duration plusOrMinus) {
    return new PopulationBuilder(wrapped.uniformPauses(toScalaDuration(plusOrMinus)));
  }

  /**
   * Use pauses configured with a given {@link PauseType}
   *
   * @param pauseType the pause type
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder pauses(@Nonnull PauseType pauseType) {
    return new PopulationBuilder(wrapped.pauses(pauseType.asScala()));
  }

  /**
   * Define the optional throttling profile
   *
   * @param throttleSteps the throttling profile steps
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder throttle(@Nonnull ThrottleStep... throttleSteps) {
    return throttle(Arrays.asList(throttleSteps));
  }

  /**
   * Define the optional throttling profile
   *
   * @param throttleSteps the throttling profile steps
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder throttle(@Nonnull List<ThrottleStep> throttleSteps) {
    return new PopulationBuilder(
        wrapped.throttle(
            toScalaSeq(
                throttleSteps.stream().map(ThrottleStep::asScala).collect(Collectors.toList()))));
  }

  /**
   * Disable the injection profile sharding that happens normally when running with Gatling
   * Enterprise. Only effective when the test is running with Gatling Enterprise, noop otherwise.
   *
   * @return a new PopulationBuilder
   */
  @Nonnull
  public PopulationBuilder noShard() {
    return new PopulationBuilder(wrapped.noShard());
  }
}
