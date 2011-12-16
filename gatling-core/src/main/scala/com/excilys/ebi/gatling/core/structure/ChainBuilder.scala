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
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.Action

/**
 * ChainBuilder class companion
 */
object ChainBuilder {
	/**
	 * DSL helper that creates a new ChainBuilder
	 */
	def chain = new ChainBuilder(Nil, null)
}
/**
 * This class defines chain related methods
 *
 * @param actionBuilders the builders that represent the chain of actions of a scenario/chain
 * @param next the action that will be executed after this chain
 */
class ChainBuilder(actionBuilders: List[AbstractActionBuilder], next: Action)
		extends AbstractStructureBuilder[ChainBuilder](actionBuilders) {

	private[core] def newInstance(actionBuilders: List[AbstractActionBuilder]) = new ChainBuilder(actionBuilders, next)

	private[core] def getInstance = this

	/**
	 * Method that sets next action (used for chains)
	 *
	 * @param next the action to be executed after the chain
	 * @return the last built action
	 */
	private[core] def withNext(next: Action) = new ChainBuilder(actionBuilders, next)

	/**
	 * Method that actually builds the scenario
	 *
	 * @param scenarioId the id of the current scenario
	 * @return the first action of the scenario to be executed
	 */
	private[core] def build: Action = buildActions(next)
}