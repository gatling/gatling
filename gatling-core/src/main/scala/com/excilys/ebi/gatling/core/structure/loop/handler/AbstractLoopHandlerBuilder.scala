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
package com.excilys.ebi.gatling.core.structure.loop.handler

import com.excilys.ebi.gatling.core.action.builder.ActionBuilder
import com.excilys.ebi.gatling.core.structure.AbstractStructureBuilder

/**
 * This class is a model for the Loop handler builders
 *
 * These builders are used to create the right structure of actors to allow loops
 *
 * @param structureBuilder the structure builder on which loop was called
 */
abstract class AbstractLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B) {
	private[core] def build: B

	/**
	 * This method adds the actionBuilders of the loop to the structure builder on which loop was called
	 *
	 * @param actionBuilders the list of actions that form the loop
	 */
	private[core] def doBuild(actionBuilders: List[ActionBuilder]): B = structureBuilder.addActionBuilders(actionBuilders)
}