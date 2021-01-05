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

package io.gatling.core.scenario

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.throttle.{ ThrottleStep, Throttling, Throttlings }
import io.gatling.core.pause._
import io.gatling.core.protocol.{ Protocol, ProtocolComponentsRegistries, Protocols }
import io.gatling.core.session.Expression
import io.gatling.core.structure.PopulationBuilder

abstract class Simulation {

  private var _populationBuilders: List[PopulationBuilder] = Nil
  private var _globalProtocols: Protocols = Map.empty
  private var _assertions = Seq.empty[Assertion]
  private var _maxDuration: Option[FiniteDuration] = None
  private var _globalPauseType: PauseType = Constant
  private var _globalThrottleSteps: Iterable[ThrottleStep] = Nil
  private var _beforeSteps: List[() => Unit] = Nil
  private var _afterSteps: List[() => Unit] = Nil

  def before(step: => Unit): Unit =
    _beforeSteps = _beforeSteps ::: List(() => step)

  def setUp(populationBuilders: PopulationBuilder*): SetUp = setUp(populationBuilders.toList)

  def setUp(populationBuilders: List[PopulationBuilder]): SetUp = {
    if (_populationBuilders.nonEmpty)
      throw new UnsupportedOperationException("setUp can only be called once")
    _populationBuilders = populationBuilders
    new SetUp
  }

  def after(step: => Unit): Unit =
    _afterSteps = _afterSteps ::: List(() => step)

  class SetUp {

    def protocols(ps: Protocol*): SetUp = protocols(ps.toIterable)

    def protocols(ps: Iterable[Protocol]): SetUp = {
      _globalProtocols = _globalProtocols ++ Protocol.indexByType(ps)
      this
    }

    def assertions(asserts: Assertion*): SetUp = assertions(asserts.toIterable)

    def assertions(asserts: Iterable[Assertion]): SetUp = {
      _assertions = _assertions ++ asserts
      this
    }

    def maxDuration(duration: FiniteDuration): SetUp = {
      _maxDuration = Some(duration)
      this
    }

    def throttle(throttleSteps: ThrottleStep*): SetUp = throttle(throttleSteps.toIterable)

    def throttle(throttleSteps: Iterable[ThrottleStep]): SetUp = {
      _globalThrottleSteps = throttleSteps
      this
    }

    def disablePauses: SetUp = pauses(Disabled)
    def constantPauses: SetUp = pauses(Constant)
    def exponentialPauses: SetUp = pauses(Exponential)
    def customPauses(custom: Expression[Long]): SetUp = pauses(new Custom(custom))
    def uniformPauses(plusOrMinus: Double): SetUp = pauses(new UniformPercentage(plusOrMinus))
    def uniformPauses(plusOrMinus: FiniteDuration): SetUp = pauses(new UniformDuration(plusOrMinus))
    def pauses(pauseType: PauseType): SetUp = {
      _globalPauseType = pauseType
      this
    }
  }

  private[gatling] def params(configuration: GatlingConfiguration): SimulationParams = {

    val rootPopulationBuilders = _populationBuilders
    require(rootPopulationBuilders.nonEmpty, "No scenario set up")

    val childrenPopulationBuilders = PopulationBuilder.groupChildrenByParent(rootPopulationBuilders)
    val allPopulationBuilders = rootPopulationBuilders ++ childrenPopulationBuilders.values.flatten

    val duplicates =
      allPopulationBuilders
        .groupBy(_.scenarioBuilder.name)
        .collect { case (name, scns) if scns.size > 1 => name }
    require(duplicates.isEmpty, s"Scenario names must be unique but found duplicates: $duplicates")

    allPopulationBuilders.foreach { scn =>
      require(scn.scenarioBuilder.name.nonEmpty, "Scenario name cannot be empty")
      require(scn.scenarioBuilder.actionBuilders.nonEmpty, s"Scenario ${scn.scenarioBuilder.name} is empty")
    }

    val scenarioThrottlings: Map[String, Throttling] = allPopulationBuilders.flatMap { populationBuilder =>
      val steps = populationBuilder.scenarioThrottleSteps

      if (steps.isEmpty) {
        Nil
      } else {
        List(populationBuilder.scenarioBuilder.name -> Throttling(steps))
      }
    }.toMap

    val globalThrottling =
      if (_globalThrottleSteps.isEmpty) {
        None
      } else {
        Some(Throttling(_globalThrottleSteps))
      }

    val maxDuration = {
      val globalThrottlingMaxDuration = globalThrottling.map(_.duration)
      val scenarioThrottlingMaxDurations = scenarioThrottlings.values.map(_.duration).toList

      _maxDuration.map(List(_)).getOrElse(Nil) ::: globalThrottlingMaxDuration.map(List(_)).getOrElse(Nil) ::: scenarioThrottlingMaxDurations match {
        case Nil => None
        case nel => Some(nel.min)
      }
    }

    new SimulationParams(
      getClass.getName,
      rootPopulationBuilders,
      childrenPopulationBuilders,
      _globalProtocols,
      _globalPauseType,
      Throttlings(globalThrottling, scenarioThrottlings),
      maxDuration,
      _assertions
    )
  }

  private[gatling] def executeBefore(): Unit = _beforeSteps.foreach(_.apply())
  private[gatling] def executeAfter(): Unit = _afterSteps.foreach(_.apply())
}

final class SimulationParams(
    val name: String,
    val rootPopulationBuilders: List[PopulationBuilder],
    val childrenPopulationBuilders: Map[String, List[PopulationBuilder]],
    val globalProtocols: Protocols,
    val globalPauseType: PauseType,
    val throttlings: Throttlings,
    val maxDuration: Option[FiniteDuration],
    val assertions: Seq[Assertion]
) {

  private def buildScenario(populationBuilder: PopulationBuilder, coreComponents: CoreComponents, protocolComponentsRegistries: ProtocolComponentsRegistries) =
    populationBuilder.build(coreComponents, protocolComponentsRegistries, globalPauseType, throttlings.global)

  def scenarios(coreComponents: CoreComponents): Scenarios = {
    val protocolComponentsRegistries = new ProtocolComponentsRegistries(coreComponents, globalProtocols)
    val rootScenarios = rootPopulationBuilders.map(buildScenario(_, coreComponents, protocolComponentsRegistries))
    val childrenScenarios = childrenPopulationBuilders.view.mapValues(_.map(buildScenario(_, coreComponents, protocolComponentsRegistries))).toMap

    new Scenarios(rootScenarios, childrenScenarios)
  }
}
