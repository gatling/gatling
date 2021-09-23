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

import io.gatling.core.config.GatlingConfiguration;
import io.gatling.core.protocol.Protocol;
import io.gatling.core.scenario.Simulation$;
import io.gatling.core.scenario.SimulationParams;
import scala.Option;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public abstract class Simulation {

  private List<PopulationBuilder> _populationBuilders;
  private Map<Class<? extends Protocol>, Protocol> _globalProtocols = new HashMap<>();
  private List<Assertion> _assertions = new ArrayList<>();
  private Duration _maxDuration;
  private PauseType _globalPauseType = PauseType.Constant;
  private List<ThrottleStep> _globalThrottleSteps = new ArrayList<>();

  public void before() {
  }

  public void after() {
  }


  public SetUp setUp(PopulationBuilder... populationBuilders) {
    return setUp(Arrays.asList(populationBuilders));
  }

  public SetUp setUp(List<PopulationBuilder> populationBuilders) {
    if (_populationBuilders != null) {
      throw new UnsupportedOperationException("Can only call setUp once");
    }
    _populationBuilders = populationBuilders;
    return new SetUp();
  }

  public class SetUp {

    private SetUp() {
    }

    public SetUp protocols(ProtocolBuilder... protocols) {
      return protocols(Arrays.asList(protocols));
    }

    public SetUp protocols(List<ProtocolBuilder> protocols) {
      _globalProtocols = protocols.stream().map(ProtocolBuilder::protocol).collect(Collectors.toMap(Protocol::getClass, Function.identity()));
      return this;
    }

    public SetUp assertions(Assertion... assertions) {
      return assertions(Arrays.asList(assertions));
    }

    public SetUp assertions(List<Assertion> assertions) {
      _assertions = assertions;
      return this;
    }

    public SetUp maxDuration(int duration) {
      return maxDuration(Duration.ofSeconds(duration));
    }

    public SetUp maxDuration(Duration duration) {
      _maxDuration = duration;
      return this;
    }

    public SetUp throttle(ThrottleStep... throttleSteps) {
      return throttle(Arrays.asList(throttleSteps));
    }

    public SetUp throttle(List<ThrottleStep> throttleSteps) {
      _globalThrottleSteps = throttleSteps;
      return this;
    }

    public SetUp disablePauses() {
      return pauses(PauseType.Disabled);
    }

    public SetUp constantPauses() {
      return pauses(PauseType.Constant);
    }

    public SetUp exponentialPauses() {
      return pauses(PauseType.Exponential);
    }

    public SetUp customPauses(Function<Session, Long> f) {
      return pauses(new PauseType.Custom(toUntypedGatlingSessionFunction(f)));
    }

    public SetUp uniformPauses(double plusOrMinus) {
      return pauses(new PauseType.UniformPercentage(plusOrMinus));
    }

    public SetUp uniformPauses(Duration plusOrMinus) {
      return pauses(new PauseType.UniformDuration(plusOrMinus));
    }

    public SetUp pauses(PauseType pauseType) {
      _globalPauseType = pauseType;
      return this;
    }
  }

  public SimulationParams params(GatlingConfiguration configuration) {
    return Simulation$.MODULE$.params(
      getClass().getName(),
      toScalaSeq(_populationBuilders.stream().map(PopulationBuilder::asScala).collect(Collectors.toList())).toList(),
      toScalaMap(_globalProtocols),
      toScalaSeq(_assertions.stream().map(Assertion::asScala).collect(Collectors.toList())),
      Option.apply(_maxDuration).map(duration -> toScalaDuration(duration)),
      _globalPauseType.asScala(),
      toScalaSeq(_globalThrottleSteps.stream().map(ThrottleStep::asScala).collect(Collectors.toList())),
      () -> {
        before();
        return null;
      },
      () -> {
        after();
        return null;
      },
      configuration
    );
  }
}
