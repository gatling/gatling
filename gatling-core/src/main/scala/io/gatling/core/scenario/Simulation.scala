/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.scenario

import scala.concurrent.duration.{ Duration, FiniteDuration }

import io.gatling.core.assertion.{ Assertion, Metric }
import io.gatling.core.config.{ Protocol, Protocols }
import io.gatling.core.controller.Timings
import io.gatling.core.controller.throttle.{ ThrottlingBuilder, ThrottlingProtocol }
import io.gatling.core.pause.{ Constant, Custom, Disabled, Exponential, PauseProtocol, PauseType, UniformDuration, UniformPercentage }
import io.gatling.core.session.Expression
import io.gatling.core.structure.PopulatedScenarioBuilder

abstract class Simulation {

	private[core] var _scenarios = Seq.empty[PopulatedScenarioBuilder]
	private[core] var _globalProtocols = Protocols()
	private[core] var _assertions = Seq.empty[Assertion]
	private[core] var _maxDuration: Option[FiniteDuration] = None
	private[core] var _globalThrottling: Option[ThrottlingProtocol] = None
	private[core] var _beforeSteps: List[() => Unit] = Nil
	private[core] var _afterSteps: List[() => Unit] = Nil

	def scenarios: Seq[Scenario] = {
		require(!_scenarios.isEmpty, "No scenario set up")
		_scenarios.foreach(scn => require(!scn.scenarioBuilder.actionBuilders.isEmpty, s"Scenario ${scn.scenarioBuilder.name} is empty"))
		_scenarios.map(_.build(_globalProtocols))
	}

	def assertions = _assertions
	def timings = {
		val perScenarioThrottlings: Map[String, ThrottlingProtocol] = _scenarios
			.map(scn => scn
				.populationProtocols.getProtocol[ThrottlingProtocol]
				.map(throttling => scn.scenarioBuilder.name -> throttling)).flatten.toMap
		Timings(_maxDuration, _globalThrottling, perScenarioThrottlings)
	}

	def before(step: => Unit) {
		_beforeSteps = _beforeSteps ::: List(() => step)
	}

	def setUp(scenarios: PopulatedScenarioBuilder*) = {
		_scenarios = scenarios.toList
		new SetUp
	}

	def after(step: => Unit) {
		_afterSteps = _afterSteps ::: List(() => step)
	}

	class SetUp {

		def protocols(ps: Protocol*) = {
			_globalProtocols = _globalProtocols ++ ps
			this
		}

		def assertions[T: Numeric](metrics: Metric[T]*) = {
			_assertions = metrics.flatMap(_.assertions)
			this
		}

		def maxDuration(duration: FiniteDuration) = {
			_maxDuration = Some(duration)
			this
		}

		def throttle(throttlingBuilders: ThrottlingBuilder*) = {

			val steps = throttlingBuilders.toList.map(_.steps).reverse.flatten
			val throttling = ThrottlingProtocol(ThrottlingBuilder(steps).build)
			_globalThrottling = Some(throttling)
			_globalProtocols = _globalProtocols + throttling
			this
		}

		def disablePauses = pauses(Disabled)
		def constantPauses = pauses(Constant)
		def exponentialPauses = pauses(Exponential)
		def customPauses(custom: Expression[Long]) = pauses(Custom(custom))
		def uniformPauses(plusOrMinus: Double) = pauses(UniformPercentage(plusOrMinus))
		def uniformPauses(plusOrMinus: Duration) = pauses(UniformDuration(plusOrMinus))
		def pauses(pauseType: PauseType) = {
			_globalProtocols = _globalProtocols + PauseProtocol(pauseType)
			this
		}
	}
}
