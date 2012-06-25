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

import java.lang.System.currentTimeMillis
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder.whileActionBuilder
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.structure.{ ChainBuilder, AbstractStructureBuilder }

/**
 * This builder creates a duration loop, using a WhileAction
 *
 * @constructor constructs a DurationLoopHandlerBuilder
 * @param structureBuilder the structure builder on which loop was called
 * @param chain the chain of actions that should be repeated
 * @param durationValue the value of the duration
 * @param durationUnit the time unit of the duration
 * @param counterName the name of the counter for this loop
 */
class DurationLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, durationValue: Int, durationUnit: TimeUnit, counterName: Option[String])
		extends AbstractLoopHandlerBuilder[B](structureBuilder) {

	/**
	 * Actually adds the current duration loop to the structure builder
	 */
	private[core] def build: B = {
		val loopCounterName = counterName.getOrElse(randomUUID.toString)
		doBuild(
			List(whileActionBuilder
				.withCondition((session: Session) => (currentTimeMillis - session.getTimerValue(loopCounterName)) <= durationUnit.toMillis(durationValue))
				.withLoopNext(chain)
				.withCounterName(loopCounterName)))
	}
}