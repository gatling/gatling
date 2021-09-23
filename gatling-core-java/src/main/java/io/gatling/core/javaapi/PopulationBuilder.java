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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public class PopulationBuilder {
  private final io.gatling.core.structure.PopulationBuilder wrapped;

  public PopulationBuilder(io.gatling.core.structure.PopulationBuilder wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.core.structure.PopulationBuilder asScala() {
    return wrapped;
  }

  public PopulationBuilder protocols(ProtocolBuilder... protocols) {
    return protocols(Arrays.asList(protocols));
  }

  public PopulationBuilder protocols(List<ProtocolBuilder> protocols) {
    return new PopulationBuilder(wrapped.protocols(toScalaSeq(protocols.stream().map(ProtocolBuilder::protocol).collect(Collectors.toList()))));
  }

  public PopulationBuilder andThen(PopulationBuilder... children) {
    return andThen(Arrays.asList(children));
  }

  public PopulationBuilder andThen(List<PopulationBuilder> children) {
    return new PopulationBuilder(wrapped.andThen(toScalaSeq(children.stream().map(PopulationBuilder::asScala).collect(Collectors.toList()))));
  }

  public PopulationBuilder disablePauses() {
    return new PopulationBuilder(wrapped.disablePauses());
  }

  public PopulationBuilder constantPauses() {
    return new PopulationBuilder(wrapped.constantPauses());
  }

  public PopulationBuilder exponentialPauses() {
    return new PopulationBuilder(wrapped.exponentialPauses());
  }

  public PopulationBuilder customPauses(Function<Session, Long> custom) {
    return new PopulationBuilder(wrapped.customPauses(toUntypedGatlingSessionFunction(custom)));
  }

  public PopulationBuilder uniformPauses(double plusOrMinus) {
    return new PopulationBuilder(wrapped.uniformPauses(plusOrMinus));
  }

  public PopulationBuilder uniformPauses(int plusOrMinus) {
    return uniformPauses(Duration.ofSeconds(plusOrMinus));
  }

  public PopulationBuilder uniformPauses(Duration plusOrMinus) {
    return new PopulationBuilder(wrapped.uniformPauses(toScalaDuration(plusOrMinus)));
  }

  public PopulationBuilder pauses(PauseType pauseType) {
    return new PopulationBuilder(wrapped.pauses(pauseType.asScala()));
  }

  public PopulationBuilder throttle(ThrottleStep... throttleSteps) {
    return throttle(Arrays.asList(throttleSteps));
  }

  public PopulationBuilder throttle(List<ThrottleStep> throttleSteps) {
    return new PopulationBuilder(wrapped.throttle(toScalaSeq(throttleSteps.stream().map(ThrottleStep::asScala).collect(Collectors.toList()))));
  }

  public PopulationBuilder noShard() {
    return new PopulationBuilder(wrapped.noShard());
  }
}
