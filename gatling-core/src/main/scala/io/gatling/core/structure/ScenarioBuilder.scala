/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.core.structure

import scala.concurrent.duration.Duration

import io.gatling.core.CoreComponents
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.controller.inject.{ InjectionProfile, InjectionProfileFactory, MetaInjectionProfile }
import io.gatling.core.controller.throttle.{ ThrottleStep, Throttling }
import io.gatling.core.pause._
import io.gatling.core.protocol.{ Protocol, ProtocolComponentsRegistries, ProtocolComponentsRegistry, Protocols }
import io.gatling.core.scenario.Scenario
import io.gatling.core.session.Expression

import com.typesafe.scalalogging.LazyLogging

/**
 * The scenario builder is used in the DSL to define the scenario
 *
 * @param name the name of the scenario
 * @param actionBuilders the list of all the actions that compose the scenario
 */
case class ScenarioBuilder(name: String, actionBuilders: List[ActionBuilder] = Nil) extends StructureBuilder[ScenarioBuilder] {

  private[core] def newInstance(actionBuilders: List[ActionBuilder]) = copy(actionBuilders = actionBuilders)

  def inject[T: InjectionProfileFactory](is: T, moreIss: T*): PopulationBuilder = inject[T](Seq(is) ++ moreIss)

  def inject[T: InjectionProfileFactory](iss: Iterable[T]): PopulationBuilder = {
    require(iss.nonEmpty, "Calling inject with empty injection steps")
    PopulationBuilder(this, implicitly[InjectionProfileFactory[T]].profile(iss))
  }

  def inject(meta: MetaInjectionProfile): PopulationBuilder =
    PopulationBuilder(this, meta.profile)
}

case class PopulationBuilder(
    scenarioBuilder:       ScenarioBuilder,
    injectionProfile:      InjectionProfile,
    scenarioProtocols:     Protocols              = Protocols(),
    scenarioThrottleSteps: Iterable[ThrottleStep] = Nil,
    pauseType:             Option[PauseType]      = None
)
  extends LazyLogging {

  def protocols(protocols: Protocol*): PopulationBuilder = copy(scenarioProtocols = this.scenarioProtocols ++ protocols)

  def disablePauses: PopulationBuilder = pauses(Disabled)
  def constantPauses: PopulationBuilder = pauses(Constant)
  def exponentialPauses: PopulationBuilder = pauses(Exponential)
  def customPauses(custom: Expression[Long]): PopulationBuilder = pauses(Custom(custom))
  def uniformPauses(plusOrMinus: Double): PopulationBuilder = pauses(UniformPercentage(plusOrMinus))
  def uniformPauses(plusOrMinus: Duration): PopulationBuilder = pauses(UniformDuration(plusOrMinus))
  def pauses(pauseType: PauseType): PopulationBuilder = copy(pauseType = Some(pauseType))

  def throttle(throttleSteps: ThrottleStep*): PopulationBuilder = throttle(throttleSteps.toIterable)

  def throttle(throttleSteps: Iterable[ThrottleStep]): PopulationBuilder = {
    require(throttleSteps.nonEmpty, s"Scenario '${scenarioBuilder.name}' has an empty throttling definition.")
    copy(scenarioThrottleSteps = throttleSteps)
  }

  /**
   * @param coreComponents the CoreComponents
   * @param protocolComponentsRegistries the ProtocolComponents registries
   * @param globalPauseType the pause type
   * @param globalThrottling the optional throttling profile
   * @return the scenario
   */
  private[core] def build(coreComponents: CoreComponents, protocolComponentsRegistries: ProtocolComponentsRegistries, globalPauseType: PauseType, globalThrottling: Option[Throttling]): Scenario = {

    val resolvedPauseType =
      if (scenarioThrottleSteps.nonEmpty || globalThrottling.isDefined) {
        logger.info("Throttle is enabled, disabling pauses")
        Disabled
      } else {
        pauseType.getOrElse(globalPauseType)
      }

    val protocolComponentsRegistry = protocolComponentsRegistries.scenarioRegistry(scenarioProtocols)

    val ctx = ScenarioContext(coreComponents, protocolComponentsRegistry, resolvedPauseType, globalThrottling.isDefined || scenarioThrottleSteps.nonEmpty)

    val entry = scenarioBuilder.build(ctx, coreComponents.exit)

    Scenario(scenarioBuilder.name, entry, protocolComponentsRegistry.onStart, protocolComponentsRegistry.onExit, injectionProfile, ctx)
  }
}

case class ScenarioContext(
    coreComponents:             CoreComponents,
    protocolComponentsRegistry: ProtocolComponentsRegistry,
    pauseType:                  PauseType,
    throttled:                  Boolean
)
