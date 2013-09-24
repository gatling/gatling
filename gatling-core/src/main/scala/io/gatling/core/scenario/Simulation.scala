/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import io.gatling.core.config.Protocol
import io.gatling.core.controller.Timings
import io.gatling.core.controller.throttle.{ ThrottlingBuilder, ThrottlingProtocol, ThrottlingSupport }
import io.gatling.core.feeder.FeederSupport
import io.gatling.core.pause.{ Constant, Custom, Disabled, Exponential, PauseProtocol, PauseType, UniformDuration, UniformPercentage }
import io.gatling.core.session.Expression
import io.gatling.core.structure.ProfiledScenarioBuilder

abstract class Simulation {

	private[scenario] var _scenarios = Seq.empty[ProfiledScenarioBuilder]
	private[scenario] var _globalProtocols = List.empty[Protocol]
	private[scenario] var _assertions = Seq.empty[Assertion]
	private[scenario] var _maxDuration: Option[FiniteDuration] = None
	private[scenario] var _globalThrottling: Option[ThrottlingProtocol] = None

	def scenarios: Seq[Scenario] = {
		require(!_scenarios.isEmpty, "No scenario set up")
		_scenarios.map(_.build(_globalProtocols))
	}

	def perScenarioThrottlings = _scenarios.map(scn => scn.protocols.collect { case p: ThrottlingProtocol => (scn.scenarioBuilder.name -> p) }).flatten.toMap
	def assertions = _assertions
	def timings = Timings(_maxDuration, _globalThrottling, perScenarioThrottlings)

	def setUp(scenario: ProfiledScenarioBuilder, scenarios: ProfiledScenarioBuilder*) = {
		_scenarios = scenario :: scenarios.toList
		new SetUp
	}

	class SetUp {

		def protocols(protocol: Protocol, protocols: Protocol*) = {
			_globalProtocols = protocol :: protocols.toList ::: _globalProtocols
			this
		}

		def assertions(metric: Metric, metrics: Metric*) = {
			_assertions = metric.assertions ++ metrics.flatMap(_.assertions)
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
			_globalProtocols = throttling :: _globalProtocols
			this
		}

		def disablePauses = pauses(Disabled)
		def constantPauses = pauses(Constant)
		def exponentialPauses = pauses(Exponential)
		def customPauses(custom: Expression[Long]) = pauses(Custom(custom))
		def uniform(plusOrMinus: Double) = pauses(UniformPercentage(plusOrMinus))
		def uniform(plusOrMinus: Duration) = pauses(UniformDuration(plusOrMinus))
		def pauses(pauseType: PauseType) = {
			_globalProtocols = PauseProtocol(pauseType) :: _globalProtocols
			this
		}
	}
}
