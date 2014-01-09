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
package io.gatling.core.structure

import scala.concurrent.duration.Duration

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.action.UserEnd
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.{ Protocol, ProtocolRegistry }
import io.gatling.core.controller.inject.{ InjectionProfile, InjectionStep }
import io.gatling.core.controller.throttle.{ ThrottlingBuilder, ThrottlingProtocol }
import io.gatling.core.pause.{ Constant, Custom, Disabled, Exponential, PauseProtocol, PauseType, UniformDuration, UniformPercentage }
import io.gatling.core.scenario.Scenario
import io.gatling.core.session.Expression

/**
 * The scenario builder is used in the DSL to define the scenario
 *
 * @param name the name of the scenario
 * @param actionBuilders the list of all the actions that compose the scenario
 */
case class ScenarioBuilder(name: String, actionBuilders: List[ActionBuilder] = Nil, protocolRegistry: ProtocolRegistry = new ProtocolRegistry(Map.empty)) extends StructureBuilder[ScenarioBuilder] {

	private[core] def newInstance(actionBuilders: List[ActionBuilder], protocolRegistry: ProtocolRegistry) = copy(actionBuilders = actionBuilders, protocolRegistry = protocolRegistry)

	def inject(iss: InjectionStep*) = {
		if (iss.isEmpty) System.err.println(s"Scenario '$name' has no injection step.")
		new ProfiledScenarioBuilder(this, InjectionProfile(iss), protocolRegistry)
	}
}

case class ProfiledScenarioBuilder(scenarioBuilder: ScenarioBuilder, injectionProfile: InjectionProfile, protocolRegistry: ProtocolRegistry) extends StrictLogging {

	def protocols(ps: Protocol*) = copy(protocolRegistry = protocolRegistry.register(ps))

	def disablePauses = pauses(Disabled)
	def constantPauses = pauses(Constant)
	def exponentialPauses = pauses(Exponential)
	def customPauses(custom: Expression[Long]) = pauses(Custom(custom))
	def uniformPauses(plusOrMinus: Double) = pauses(UniformPercentage(plusOrMinus))
	def uniformPauses(plusOrMinus: Duration) = pauses(UniformDuration(plusOrMinus))
	def pauses(pauseType: PauseType) = protocols(PauseProtocol(pauseType))

	def throttle(throttlingBuilders: ThrottlingBuilder*) = {
		if (throttlingBuilders.isEmpty) System.err.println(s"Scenario '${scenarioBuilder.name}' has an empty throttling definition.")
		val steps = throttlingBuilders.toList.map(_.steps).reverse.flatten
		protocols(ThrottlingProtocol(ThrottlingBuilder(steps).build))
	}

	/**
	 * @param protocolRegistry
	 * @return the scenario
	 */
	private[core] def build(globalProtocols: ProtocolRegistry): Scenario = {

		val newProtocols = (globalProtocols ++ protocolRegistry).getProtocol[ThrottlingProtocol] match {
			case Some(_) =>
				logger.info("Throttle is enabled, disabling pauses")
				protocolRegistry.register(PauseProtocol(Disabled))
			case None => protocolRegistry
		}

		newProtocols.warmUp

		val entryPoint = scenarioBuilder.build(UserEnd.instance, newProtocols)
		new Scenario(scenarioBuilder.name, entryPoint, injectionProfile)
	}
}
