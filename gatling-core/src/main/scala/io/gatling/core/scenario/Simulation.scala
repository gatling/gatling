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
package io.gatling.core.scenario

import scala.concurrent.duration.{ Duration, FiniteDuration }

import akka.actor.ActorSystem
import io.gatling.core.CoreComponents
import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.throttle.{ ThrottleStep, Throttling }
import io.gatling.core.pause.{ Constant, Custom, Disabled, Exponential, PauseType, UniformDuration, UniformPercentage }
import io.gatling.core.protocol.{ ProtocolComponentsRegistry, Protocols, Protocol }
import io.gatling.core.session.Expression
import io.gatling.core.structure.PopulationBuilder

abstract class Simulation {

  private var _populationBuilders: List[PopulationBuilder] = Nil
  private var _globalProtocols: Protocols = Protocols()
  private var _assertions = Seq.empty[Assertion]
  private var _maxDuration: Option[FiniteDuration] = None
  private var _globalPauseType: PauseType = Constant
  private var _globalThrottling: Option[Throttling] = None
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
      _globalProtocols = _globalProtocols ++ ps
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
      _globalThrottling = Some(Throttling(throttleSteps))
      this
    }

    def disablePauses = pauses(Disabled)
    def constantPauses = pauses(Constant)
    def exponentialPauses = pauses(Exponential)
    def customPauses(custom: Expression[Long]) = pauses(Custom(custom))
    def uniformPauses(plusOrMinus: Double) = pauses(UniformPercentage(plusOrMinus))
    def uniformPauses(plusOrMinus: Duration) = pauses(UniformDuration(plusOrMinus))
    def pauses(pauseType: PauseType): SetUp = {
      _globalPauseType = pauseType
      this
    }
  }

  private[core] def params = {

    require(_populationBuilders.nonEmpty, "No scenario set up")
    val duplicates = _populationBuilders.groupBy(_.scenarioBuilder.name).collect { case (name, scns) if scns.size > 1 => name }
    require(duplicates.isEmpty, s"Scenario names must be unique but found duplicates: $duplicates")
    _populationBuilders.foreach(scn => require(scn.scenarioBuilder.actionBuilders.nonEmpty, s"Scenario ${scn.scenarioBuilder.name} is empty"))

    val scenarioThrottlings: Map[String, Throttling] = _populationBuilders
      .flatMap(scn => scn.scenarioThrottling.map(t => scn.scenarioBuilder.name -> t)).toMap

    val maxDuration = {

      val globalThrottlingMaxDuration = _globalThrottling.map(_.duration)
      val scenarioThrottlingMaxDurations = scenarioThrottlings.values.map(_.duration).toList

      _maxDuration.map(List(_)).getOrElse(Nil) ::: globalThrottlingMaxDuration.map(List(_)).getOrElse(Nil) ::: scenarioThrottlingMaxDurations match {
        case Nil => None
        case nel => Some(nel.min)
      }
    }

    SimulationParams(getClass.getName,
      _populationBuilders,
      _globalProtocols,
      _globalPauseType,
      _globalThrottling,
      scenarioThrottlings,
      maxDuration,
      _assertions,
      _beforeSteps,
      _afterSteps)
  }
}

case class SimulationParams(name: String,
                            populationBuilders: List[PopulationBuilder],
                            globalProtocols: Protocols,
                            globalPauseType: PauseType,
                            globalThrottling: Option[Throttling],
                            scenarioThrottlings: Map[String, Throttling],
                            maxDuration: Option[FiniteDuration],
                            assertions: Seq[Assertion],
                            beforeSteps: List[() => Unit],
                            afterSteps: List[() => Unit]) {

  def scenarios(system: ActorSystem, coreComponents: CoreComponents)(implicit configuration: GatlingConfiguration): List[Scenario] = {

    val protocolComponentsRegistry = new ProtocolComponentsRegistry(system, coreComponents, globalProtocols)

    populationBuilders.map(_.build(system, coreComponents, protocolComponentsRegistry, globalProtocols, globalPauseType, globalThrottling))
  }
}
