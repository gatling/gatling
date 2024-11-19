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

import static io.gatling.javaapi.core.internal.Converters.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.gatling.core.config.GatlingConfiguration;
import io.gatling.core.protocol.Protocol;
import io.gatling.core.scenario.SimulationParams;
import io.gatling.javaapi.core.internal.Converters;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import scala.Option;

/**
 * The class your own Simulations must extend.
 *
 * <p>On contrary to other Gatling DSL components, this class is mutable
 */
public abstract class Simulation {

  private List<PopulationBuilder> _populationBuilders = new ArrayList<>();
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
  @NonNull
  public SetUp setUp(@NonNull PopulationBuilder... populationBuilders) {
    return setUp(Arrays.asList(populationBuilders));
  }

  /**
   * Must be called inside the constructor
   *
   * @param populationBuilders the PopulationBuilder to be executed in this Simulation
   * @return a setup to possibly configure some protocols or assertions
   */
  @NonNull
  public SetUp setUp(@NonNull List<PopulationBuilder> populationBuilders) {
    if (!_populationBuilders.isEmpty()) {
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
    @NonNull
    public SetUp protocols(@NonNull ProtocolBuilder... protocols) {
      return protocols(Arrays.asList(protocols));
    }

    /**
     * Define the desired protocol configurations
     *
     * @param protocols the protocols
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp protocols(@NonNull List<ProtocolBuilder> protocols) {
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
    @NonNull
    public SetUp assertions(@NonNull Assertion... assertions) {
      return assertions(Arrays.asList(assertions));
    }

    /**
     * Define the desired assertions
     *
     * @param assertions the assertions
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp assertions(@NonNull List<Assertion> assertions) {
      _assertions = assertions;
      return this;
    }

    /**
     * Define the run max duration
     *
     * @param duration the max duration in seconds
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp maxDuration(long duration) {
      return maxDuration(Duration.ofSeconds(duration));
    }

    /**
     * Define the run max duration
     *
     * @param duration the max duration
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp maxDuration(@NonNull Duration duration) {
      _maxDuration = duration;
      return this;
    }

    /**
     * Define the throttling, meaning a maximum throughput over time
     *
     * @param throttleSteps the throttling DSL steps
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp throttle(@NonNull ThrottleStep... throttleSteps) {
      return throttle(Arrays.asList(throttleSteps));
    }

    /**
     * Define the throttling, meaning a maximum throughput over time
     *
     * @param throttleSteps the throttling DSL steps
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp throttle(@NonNull List<ThrottleStep> throttleSteps) {
      _globalThrottleSteps = throttleSteps;
      return this;
    }

    /**
     * Disable the pauses, see {@link PauseType#Disabled}
     *
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp disablePauses() {
      return pauses(PauseType.Disabled);
    }

    /**
     * Apply constant pauses, see {@link PauseType#Constant}
     *
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp constantPauses() {
      return pauses(PauseType.Constant);
    }

    /**
     * Apply exponential pauses, see {@link PauseType#Exponential}
     *
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp exponentialPauses() {
      return pauses(PauseType.Exponential);
    }

    /**
     * Apply custom pauses, see {@link PauseType.Custom}
     *
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp customPauses(@NonNull Function<Session, Long> f) {
      return pauses(new PauseType.Custom(f));
    }

    /**
     * Apply uniform pauses with half-width defined as a percentage, see {@link
     * PauseType.UniformPercentage}
     *
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp uniformPauses(double plusOrMinus) {
      return pauses(new PauseType.UniformPercentage(plusOrMinus));
    }

    /**
     * Apply uniform pauses with half-width defined as an absolute value, see {@link
     * PauseType.UniformDuration}
     *
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp uniformPauses(@NonNull Duration plusOrMinus) {
      return pauses(new PauseType.UniformDuration(plusOrMinus));
    }

    /**
     * Apply normal distribution pauses with the standard deviation defined as an absolute value,
     * see {@link PauseType.NormalWithStdDevDuration}
     *
     * @param stdDevDuration the standard deviation of the distribution.
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp normalPausesWithStdDevDuration(Duration stdDevDuration) {
      return pauses(new PauseType.NormalWithStdDevDuration(stdDevDuration));
    }

    /**
     * Apply normal distribution pauses with the standard deviation defined as a percentage of the
     * value defined in the scenario, see {@link PauseType.NormalWithPercentageDuration}
     *
     * @param stdDevPercent the standard deviation of the distribution in percents.
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp normalPausesWithPercentageDuration(double stdDevPercent) {
      return pauses(new PauseType.NormalWithPercentageDuration(stdDevPercent));
    }

    /**
     * Apply uniform pauses with a given strategy
     *
     * @param pauseType the pause type
     * @return the same mutated setup instance
     */
    @NonNull
    public SetUp pauses(@NonNull PauseType pauseType) {
      _globalPauseType = pauseType;
      return this;
    }
  }

  public SimulationParams params(
      GatlingConfiguration configuration, @Nullable String simulationName) {
    return io.gatling.core.scenario.Simulation$.MODULE$.params(
        simulationName != null ? simulationName : getClass().getName(),
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
