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

package io.gatling.core.structure

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.CoreComponents
import io.gatling.core.controller.inject.InjectionProfile
import io.gatling.core.controller.throttle.{ ThrottleStep, Throttling }
import io.gatling.core.pause._
import io.gatling.core.protocol.{ Protocol, ProtocolComponentsRegistries, Protocols }
import io.gatling.core.scenario.Scenario
import io.gatling.core.session.Expression

import com.softwaremill.quicklens._
import com.typesafe.scalalogging.LazyLogging

object PopulationBuilder {

  def groupChildrenByParent(populationBuilders: List[PopulationBuilder]): Map[String, List[PopulationBuilder]] = {
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def groupChildrenByParentRec(ancestorName: String, populationBuilders: List[PopulationBuilder]): Map[String, List[PopulationBuilder]] =
      if (populationBuilders.isEmpty) {
        Map.empty
      } else {
        Map(ancestorName -> populationBuilders) ++ populationBuilders.flatMap(pb => groupChildrenByParentRec(pb.scenarioBuilder.name, pb.children.toList))
      }

    populationBuilders.foldLeft(Map.empty[String, List[PopulationBuilder]]) { (acc, populationBuilder) =>
      acc ++ groupChildrenByParentRec(populationBuilder.scenarioBuilder.name, populationBuilder.children.toList)
    }
  }
}

final case class PopulationBuilder(
    scenarioBuilder: ScenarioBuilder,
    injectionProfile: InjectionProfile,
    scenarioProtocols: Protocols,
    scenarioThrottleSteps: Iterable[ThrottleStep],
    pauseType: Option[PauseType],
    children: Iterable[PopulationBuilder],
    shard: Boolean
) extends LazyLogging {

  def protocols(ps: Protocol*): PopulationBuilder = protocols(ps.toIterable)
  def protocols(ps: Iterable[Protocol]): PopulationBuilder = copy(scenarioProtocols = this.scenarioProtocols ++ Protocol.indexByType(ps))

  def andThen(children: PopulationBuilder*): PopulationBuilder = andThen(children.toIterable)
  def andThen(children: Iterable[PopulationBuilder]): PopulationBuilder = this.modify(_.children).using(_ ++ children)

  def disablePauses: PopulationBuilder = pauses(Disabled)
  def constantPauses: PopulationBuilder = pauses(Constant)
  def exponentialPauses: PopulationBuilder = pauses(Exponential)
  def customPauses(custom: Expression[Long]): PopulationBuilder = pauses(new Custom(custom))
  def uniformPauses(plusOrMinus: Double): PopulationBuilder = pauses(new UniformPercentage(plusOrMinus))
  def uniformPauses(plusOrMinus: FiniteDuration): PopulationBuilder = pauses(new UniformDuration(plusOrMinus))
  def pauses(pauseType: PauseType): PopulationBuilder = copy(pauseType = Some(pauseType))

  def throttle(throttleSteps: ThrottleStep*): PopulationBuilder = throttle(throttleSteps.toIterable)
  def throttle(throttleSteps: Iterable[ThrottleStep]): PopulationBuilder = {
    require(throttleSteps.nonEmpty, s"Scenario '${scenarioBuilder.name}' has an empty throttling definition.")
    copy(scenarioThrottleSteps = throttleSteps)
  }

  def noShard: PopulationBuilder = copy(shard = false)

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private[core] def build(
      coreComponents: CoreComponents,
      protocolComponentsRegistries: ProtocolComponentsRegistries,
      globalPauseType: PauseType,
      globalThrottling: Option[Throttling]
  ): Scenario = {
    val resolvedPauseType =
      if (scenarioThrottleSteps.nonEmpty || globalThrottling.isDefined) {
        logger.info("Throttle is enabled, disabling pauses")
        Disabled
      } else {
        pauseType.getOrElse(globalPauseType)
      }

    val protocolComponentsRegistry = protocolComponentsRegistries.scenarioRegistry(scenarioProtocols)

    val ctx = new ScenarioContext(coreComponents, protocolComponentsRegistry, resolvedPauseType, globalThrottling.isDefined || scenarioThrottleSteps.nonEmpty)

    val entry = scenarioBuilder.build(ctx, coreComponents.exit)

    val childrenScenarios = children.map(_.build(coreComponents, protocolComponentsRegistries, globalPauseType, globalThrottling))

    new Scenario(
      scenarioBuilder.name.trim,
      entry,
      protocolComponentsRegistry.onStart,
      protocolComponentsRegistry.onExit,
      injectionProfile,
      ctx,
      childrenScenarios
    )
  }
}
