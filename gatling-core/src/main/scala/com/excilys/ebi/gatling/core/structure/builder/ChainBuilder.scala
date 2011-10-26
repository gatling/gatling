/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.structure.builder
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.GroupActionBuilder

object ChainBuilder {
	def chain = new ChainBuilder(Nil, null, Nil)
}
class ChainBuilder(actionBuilders: List[AbstractActionBuilder], next: Action, groups: List[String])
		extends AbstractStructureBuilder[ChainBuilder](actionBuilders) {

	def newInstance(actionBuilders: List[AbstractActionBuilder]) = {
		new ChainBuilder(actionBuilders, next, groups)
	}

	def getInstance = this

	/**
	 * Method that sets next action (used for chains)
	 *
	 * @param next the action to be executed after the chain
	 * @return the last built action
	 */
	def withNext(next: Action) = new ChainBuilder(actionBuilders, next, groups)

	/**
	 * Method that sets the group of a chain
	 *
	 * @param groups the list of groups in which the chain is
	 * @return a new builder with its groups set
	 */
	def inGroups(groups: List[String]) = new ChainBuilder(actionBuilders, next, groups)

	/**
	 * Method that actually builds the scenario
	 *
	 * @param scenarioId the id of the current scenario
	 * @return the first action of the scenario to be executed
	 */
	def build: Action = buildActions(next)
}