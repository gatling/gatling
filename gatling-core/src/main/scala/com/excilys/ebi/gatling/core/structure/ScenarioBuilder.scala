/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.ActionBuilder
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder.endActionBuilder
import com.excilys.ebi.gatling.core.action.builder.StartActionBuilder.startActionBuilder
import com.excilys.ebi.gatling.core.scenario.Scenario
import com.excilys.ebi.gatling.core.scenario.configuration.{ ConfiguredScenarioBuilder, ScenarioConfiguration }

/**
 * ScenarioBuilder class companion
 */
object ScenarioBuilder {
	def scenario(scenarioName: String) = new ScenarioBuilder(scenarioName, Nil).start
}
/**
 * The scenario builder is used in the DSL to define the scenario
 *
 * @param name the name of the scenario
 * @param actionBuilders the list of all the actions that compose the scenario
 * @param next the action that will be executed after this scenario (that can be a chain as well)
 */
class ScenarioBuilder(val name: String, actionBuilders: List[ActionBuilder]) extends AbstractStructureBuilder[ScenarioBuilder](actionBuilders) {

	private[core] def newInstance(actionBuilders: List[ActionBuilder]) = {
		new ScenarioBuilder(name, actionBuilders)
	}

	private[core] def getInstance = this

	def configure = new ConfiguredScenarioBuilder(this)

	/**
	 * Method that should not be used in a script. It adds a StartAction to the scenario
	 *
	 * @return a new builder with its first action added
	 */
	private[core] def start: ScenarioBuilder = newInstance(startActionBuilder :: actionBuilders)

	/**
	 * Method that actually builds the scenario
	 *
	 * @param scenarioConfiguration the configuration of the scenario
	 * @return the scenario
	 */
	private[core] def build(scenarioConfiguration: ScenarioConfiguration): Scenario = {

		val endingScenarioBuilder = newInstance(endActionBuilder :: actionBuilders)
		val entryPoint = endingScenarioBuilder.buildChainedActions(null, scenarioConfiguration.protocolRegistry)
		new Scenario(name, entryPoint, scenarioConfiguration)
	}
}
