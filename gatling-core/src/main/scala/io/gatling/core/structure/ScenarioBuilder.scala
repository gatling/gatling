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

import scala.annotation.migration
import scala.concurrent.duration.Duration

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
case class ScenarioBuilder(name: String, actionBuilders: List[ActionBuilder] = Nil) extends StructureBuilder[ScenarioBuilder] {

	private[core] def newInstance(actionBuilders: List[ActionBuilder]) = copy(actionBuilders = actionBuilders)

	private[core] def getInstance = this

	def inject(is: InjectionStep, iss: InjectionStep*) = new ProfiledScenarioBuilder(this, InjectionProfile(is +: iss))
}

case class ProfiledScenarioBuilder(scenarioBuilder: ScenarioBuilder, injectionProfile: InjectionProfile, protocols: Map[Class[_ <: Protocol], Protocol] = Map.empty) {

	def protocols(ps: Protocol*) = copy(protocols = protocols ++ ps.map(p => p.getClass -> p))

	def disablePauses = pauses(Disabled)
	def constantPauses = pauses(Constant)
	def exponentialPauses = pauses(Exponential)
	def customPauses(custom: Expression[Long]) = pauses(Custom(custom))
	def uniform(plusOrMinus: Double) = pauses(UniformPercentage(plusOrMinus))
	def uniform(plusOrMinus: Duration) = pauses(UniformDuration(plusOrMinus))
	def pauses(pauseType: PauseType) = protocols(PauseProtocol(pauseType))

	def throttle(throttlingBuilders: ThrottlingBuilder*) = {
		val steps = throttlingBuilders.toList.map(_.steps).reverse.flatten
		protocols(ThrottlingProtocol(ThrottlingBuilder(steps).build))
	}

	/**
	 * @param protocolRegistry
	 * @return the scenario
	 */
	private[core] def build(globalProtocols: Map[Class[_ <: Protocol], Protocol]): Scenario = {

		val protocolRegistry = {
			var resolvedProtocols = globalProtocols ++ protocols
			if (resolvedProtocols.contains(classOf[ThrottlingProtocol]))
				resolvedProtocols = resolvedProtocols + (classOf[PauseProtocol] -> PauseProtocol(Disabled))

			ProtocolRegistry(resolvedProtocols.values.toSeq)
		}

		val entryPoint = scenarioBuilder.build(UserEnd.instance, protocolRegistry)
		new Scenario(scenarioBuilder.name, entryPoint, injectionProfile)
	}
}
