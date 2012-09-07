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

import java.util.UUID

import com.excilys.ebi.gatling.core.action.builder.TryMaxActionBuilder.tryMaxActionBuilder
import com.excilys.ebi.gatling.core.structure.{ AbstractStructureBuilder, ChainBuilder }

class TryMaxLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, times: Int, counterName: Option[String])
	extends AbstractLoopHandlerBuilder[B](structureBuilder) {

	/**
	 * Actually adds the current conditional loop to the structure builder
	 */
	private[core] def build: B = {
		val loopCounterName = counterName.getOrElse(UUID.randomUUID.toString)
		doBuild(List(tryMaxActionBuilder.withTimes(times).withLoopNext(chain).withCounterName(loopCounterName)))
	}
}