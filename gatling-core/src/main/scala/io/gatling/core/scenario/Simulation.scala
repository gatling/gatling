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

import scala.concurrent.duration.Duration

import io.gatling.core.config.{ Protocol, ProtocolRegistry }
import io.gatling.core.pause.{ PauseProtocol, PauseType }
import io.gatling.core.structure.{ Assertion, ChainBuilder, Metric, ProfiledScenarioBuilder }

abstract class Simulation {

	private[scenario] var _scenarios = Seq.empty[ProfiledScenarioBuilder]
	private[scenario] var _protocols = List.empty[Protocol]
	private[scenario] var _assertions = Seq.empty[Assertion]

	def scenarios: Seq[Scenario] = {
		require(!_scenarios.isEmpty, "No scenario set up")
		ProtocolRegistry.setUp(_protocols)
		_scenarios.map(_.build)
	}

	def protocols = _protocols
	def assertions = _assertions

	def setUp(scenario: ProfiledScenarioBuilder, scenarios: ProfiledScenarioBuilder*) = {
		_scenarios = scenario :: scenarios.toList
		new SetUp
	}

	class SetUp {

		def protocols(protocol: Protocol, protocols: Protocol*) = {
			_protocols = protocol :: protocols.toList ::: _protocols
			this
		}

		def assertions(metric: Metric, metrics: Metric*) = {
			_assertions = metric.assertions ++ metrics.flatMap(_.assertions)
			this
		}

		def maxDuration(duration: Duration) = {

			_scenarios = _scenarios.map { profiledScenarioBuilder =>
				val loop = ChainBuilder.empty.maxSimulationDuration(duration) {
					new ChainBuilder(profiledScenarioBuilder.scenarioBuilder.actionBuilders)
				}

				profiledScenarioBuilder.copy(scenarioBuilder = profiledScenarioBuilder.scenarioBuilder.copy(actionBuilders = loop.actionBuilders))
			}
			this
		}

		def pauses(pauseType: PauseType) = {
			_protocols = PauseProtocol(pauseType) :: _protocols
			this
		}
	}
}
