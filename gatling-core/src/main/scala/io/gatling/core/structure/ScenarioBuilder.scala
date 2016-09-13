/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import io.gatling.core.controller.inject.{ InjectionProfile, InjectionStep }
import io.gatling.core.controller.throttle.{ ThrottleStep, Throttling }
import io.gatling.core.pause._
import io.gatling.core.protocol.{ ProtocolComponentsRegistries, ProtocolComponentsRegistry, Protocols, Protocol }
import io.gatling.core.scenario.Scenario
import io.gatling.core.session.Expression

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging

/**
 * The scenario builder is used in the DSL to define the scenario
 *
 * @param name the name of the scenario
 * @param actionBuilders the list of all the actions that compose the scenario
 */
case class ScenarioBuilder(name: String, actionBuilders: List[ActionBuilder] = Nil) extends StructureBuilder[ScenarioBuilder] {

  private[core] def newInstance(actionBuilders: List[ActionBuilder]) = copy(actionBuilders = actionBuilders)

  def inject(iss: InjectionStep*): PopulationBuilder = inject(iss.toIterable)

  def inject(iss: Iterable[InjectionStep]): PopulationBuilder = {
    require(iss.nonEmpty, "Calling inject with empty injection steps")
    PopulationBuilder(this, InjectionProfile(iss))
  }
}

case class PopulationBuilder(
  scenarioBuilder:       ScenarioBuilder,
  injectionProfile:      InjectionProfile,
  scenarioProtocols:     Protocols              = Protocols(),
  scenarioThrottleSteps: Iterable[ThrottleStep] = Nil,
  pauseType:             Option[PauseType]      = None
)
    extends LazyLogging {

  def protocols(protocols: Protocol*) = copy(scenarioProtocols = this.scenarioProtocols ++ protocols)

  def disablePauses = pauses(Disabled)
  def constantPauses = pauses(Constant)
  def exponentialPauses = pauses(Exponential)
  def customPauses(custom: Expression[Long]) = pauses(Custom(custom))
  def uniformPauses(plusOrMinus: Double) = pauses(UniformPercentage(plusOrMinus))
  def uniformPauses(plusOrMinus: Duration) = pauses(UniformDuration(plusOrMinus))
  def pauses(pauseType: PauseType) = copy(pauseType = Some(pauseType))

  def throttle(throttleSteps: ThrottleStep*): PopulationBuilder = throttle(throttleSteps.toIterable)

  def throttle(throttleSteps: Iterable[ThrottleStep]): PopulationBuilder = {
    require(throttleSteps.nonEmpty, s"Scenario '${scenarioBuilder.name}' has an empty throttling definition.")
    copy(scenarioThrottleSteps = throttleSteps)
  }

  /**
   * @param system the actor system
   * @param coreComponents the CoreComponents
   * @param protocolComponentsRegistries the ProtocolComponents registries
   * @param globalPauseType the pause type
   * @param globalThrottling the optional throttling profile
   * @return the scenario
   */
  private[core] def build(system: ActorSystem, coreComponents: CoreComponents, protocolComponentsRegistries: ProtocolComponentsRegistries, globalPauseType: PauseType, globalThrottling: Option[Throttling]): Scenario = {

    val resolvedPauseType =
      if (scenarioThrottleSteps.nonEmpty || globalThrottling.isDefined) {
        logger.info("Throttle is enabled, disabling pauses")
        Disabled
      } else {
        pauseType.getOrElse(globalPauseType)
      }

    val protocolComponentsRegistry = protocolComponentsRegistries.scenarioRegistry(scenarioProtocols)

    val ctx = ScenarioContext(system, coreComponents, protocolComponentsRegistry, resolvedPauseType, globalThrottling.isDefined || scenarioThrottleSteps.nonEmpty)

    val entry = scenarioBuilder.build(ctx, coreComponents.exit)

    Scenario(scenarioBuilder.name, entry, protocolComponentsRegistry.onStart, protocolComponentsRegistry.onExit, injectionProfile, ctx)
  }
}

case class ScenarioContext(
  system:                     ActorSystem,
  coreComponents:             CoreComponents,
  protocolComponentsRegistry: ProtocolComponentsRegistry,
  pauseType:                  PauseType,
  throttled:                  Boolean
)
