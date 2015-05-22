/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import io.gatling.core.protocol.{Protocols, Protocol}

import akka.actor.ActorSystem
import io.gatling.core.CoreComponents
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.inject.{ InjectionProfile, InjectionStep }
import io.gatling.core.controller.throttle.{ ThrottleStep, Throttling }
import io.gatling.core.pause._
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

  def inject(iss: InjectionStep*): PopulationBuilder = inject(iss.toIterable)

  def inject(iss: Iterable[InjectionStep]): PopulationBuilder = {
    require(iss.nonEmpty, "Calling inject with empty injection steps")

    val defaultProtocols = actionBuilders.flatMap(_.defaultProtocols).toSet

    new PopulationBuilder(this, InjectionProfile(iss), Protocols(defaultProtocols.toList: _*))
  }
}

case class PopulationBuilder(
  scenarioBuilder: ScenarioBuilder,
  injectionProfile: InjectionProfile,
  defaultProtocols: Protocols,
  scenarioProtocols: Protocols = Protocols(),
  scenarioThrottling: Option[Throttling] = None,
  pauseType: Option[PauseType] = None)
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
    copy(scenarioThrottling = Some(Throttling(throttleSteps)))
  }

  /**
   * @param system the actor system
   * @param coreComponents the CoreComponents
   * @param globalProtocols the protocols
   * @param globalPauseType the pause type
   * @param globalThrottling the optional throttling profile
   * @return the scenario
   */
  private[core] def build(system: ActorSystem, coreComponents: CoreComponents, globalProtocols: Protocols, globalPauseType: PauseType, globalThrottling: Option[Throttling])(implicit configuration: GatlingConfiguration): Scenario = {

    val resolvedPauseType = globalThrottling.orElse(scenarioThrottling).map { _ =>
      logger.info("Throttle is enabled, disabling pauses")
      Disabled
    }.orElse(pauseType).getOrElse(globalPauseType)

    val protocols = defaultProtocols ++ globalProtocols ++ scenarioProtocols

    val ctx = ScenarioContext(coreComponents, protocols, resolvedPauseType, globalThrottling.isDefined || scenarioThrottling.isDefined)

    val entry = scenarioBuilder.build(system, ctx, coreComponents.exit)
    new Scenario(scenarioBuilder.name, entry, injectionProfile, ctx)
  }
}

case class ScenarioContext(coreComponents: CoreComponents, protocols: Protocols, pauseType: PauseType, throttled: Boolean)
