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

import io.gatling.core.config.GatlingConfiguration;
import io.gatling.core.javaapi.internal.Converters;
import io.gatling.core.protocol.Protocol;
import io.gatling.core.scenario.SimulationParams;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import scala.Option;

/**
 * The class your own Simulations must extend.
 *
 * <p>On contrary to other Gatling DSL components, this class is mutable
 */
public abstract class Simulation {

  private List<PopulationBuilder> _populationBuilders;
  private Map<Class<? extends Protocol>, Protocol> _globalProtocols = new HashMap<>();
  private List<Assertion> _assertions = new ArrayList<>();
  private Duration _maxDuration;
  private PauseType _globalPauseType = PauseType.Constant;
  private List<ThrottleStep> _globalThrottleSteps = new ArrayList<>();

  /**
   * Override to execute some arbitrary code after the Simulation is instantiated but before it's
   * executed.
   */
  public void before() {}

  /** Override to execute some arbitrary code after the Simulation has run. */
  public void after() {}

  /**
   * Must be called inside the constructor
   *
   * @param populationBuilders the PopulationBuilder to be executed in this Simulation
   * @return a setup to possibly configure some protocols or assertions
   */
  @Nonnull
  public SetUp setUp(@Nonnull PopulationBuilder... populationBuilders) {
    return setUp(Arrays.asList(populationBuilders));
  }

  /**
   * Must be called inside the constructor
   *
   * @param populationBuilders the PopulationBuilder to be executed in this Simulation
   * @return a setup to possibly configure some protocols or assertions
   */
  @Nonnull
  public SetUp setUp(@Nonnull List<PopulationBuilder> populationBuilders) {
    if (_populationBuilders != null) {
      throw new UnsupportedOperationException("Can only call setUp once");
    }
    _populationBuilders = populationBuilders;
    return new SetUp();
  }

  /**
   * The DSL component to define the desired protocols and assertions in a Simulation.
   *
   * <p>On contrary to other Gatling DSL components, this class is mutable.
   */
  public class SetUp {

    private SetUp() {}

    /**
     * Define the desired protocol configurations
     *
     * @param protocols the protocols
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp protocols(@Nonnull ProtocolBuilder... protocols) {
      return protocols(Arrays.asList(protocols));
    }

    /**
     * Define the desired protocol configurations
     *
     * @param protocols the protocols
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp protocols(@Nonnull List<ProtocolBuilder> protocols) {
      _globalProtocols =
          protocols.stream()
              .map(ProtocolBuilder::protocol)
              .collect(Collectors.toMap(Protocol::getClass, Function.identity()));
      return this;
    }

    /**
     * Define the desired assertions
     *
     * @param assertions the assertions
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp assertions(@Nonnull Assertion... assertions) {
      return assertions(Arrays.asList(assertions));
    }

    /**
     * Define the desired assertions
     *
     * @param assertions the assertions
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp assertions(@Nonnull List<Assertion> assertions) {
      _assertions = assertions;
      return this;
    }

    /**
     * Define the run max duration
     *
     * @param duration the max duration in seconds
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp maxDuration(int duration) {
      return maxDuration(Duration.ofSeconds(duration));
    }

    /**
     * Define the run max duration
     *
     * @param duration the max duration
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp maxDuration(@Nonnull Duration duration) {
      _maxDuration = duration;
      return this;
    }

    /**
     * Define the throttling, meaning a maximum throughput over time
     *
     * @param throttleSteps the throttling DSL steps
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp throttle(@Nonnull ThrottleStep... throttleSteps) {
      return throttle(Arrays.asList(throttleSteps));
    }

    /**
     * Define the throttling, meaning a maximum throughput over time
     *
     * @param throttleSteps the throttling DSL steps
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp throttle(@Nonnull List<ThrottleStep> throttleSteps) {
      _globalThrottleSteps = throttleSteps;
      return this;
    }

    /**
     * Disable the pauses, see {@link PauseType#Disabled}
     *
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp disablePauses() {
      return pauses(PauseType.Disabled);
    }

    /**
     * Apply constant pauses, see {@link PauseType#Constant}
     *
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp constantPauses() {
      return pauses(PauseType.Constant);
    }

    /**
     * Apply exponential pauses, see {@link PauseType#Exponential}
     *
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp exponentialPauses() {
      return pauses(PauseType.Exponential);
    }

    /**
     * Apply custom pauses, see {@link PauseType.Custom}
     *
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp customPauses(@Nonnull Function<Session, Long> f) {
      return pauses(new PauseType.Custom(f));
    }

    /**
     * Apply uniform pauses with half-width defined as a percentage, see {@link
     * PauseType.UniformPercentage}
     *
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp uniformPauses(double plusOrMinus) {
      return pauses(new PauseType.UniformPercentage(plusOrMinus));
    }

    /**
     * Apply uniform pauses with half-width defined as an absolute value, see {@link
     * PauseType.UniformDuration}
     *
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp uniformPauses(@Nonnull Duration plusOrMinus) {
      return pauses(new PauseType.UniformDuration(plusOrMinus));
    }

    /**
     * Apply uniform pauses with a given strategy
     *
     * @param pauseType the pause type
     * @return the same mutated setup instance
     */
    @Nonnull
    public SetUp pauses(@Nonnull PauseType pauseType) {
      _globalPauseType = pauseType;
      return this;
    }
  }

  public SimulationParams params(GatlingConfiguration configuration) {
    return io.gatling.core.scenario.Simulation$.MODULE$.params(
        getClass().getName(),
        toScalaSeq(
                _populationBuilders.stream()
                    .map(PopulationBuilder::asScala)
                    .collect(Collectors.toList()))
            .toList(),
        toScalaMap(_globalProtocols),
        toScalaSeq(_assertions.stream().map(Assertion::asScala).collect(Collectors.toList())),
        Option.apply(_maxDuration).map(Converters::toScalaDuration),
        _globalPauseType.asScala(),
        toScalaSeq(
            _globalThrottleSteps.stream().map(ThrottleStep::asScala).collect(Collectors.toList())),
        () -> {
          before();
          return null;
        },
        () -> {
          after();
          return null;
        },
        configuration);
  }
}
