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
package com.excilys.ebi.gatling.core.structure.loop.handler
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.CountBasedIterationActionBuilder._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.structure.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.ChainBuilder
import akka.actor.TypedActor
import com.excilys.ebi.gatling.core.action.Action
import akka.actor.Uuid

class TimesLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, times: Int, counterName: Option[String])
		extends AbstractLoopHandlerBuilder[B](structureBuilder) {

	def build: B = {

		val counter = counterName.getOrElse(new Uuid().toString)

		val chainActions: List[AbstractActionBuilder] = chain.getActionBuilders ::: List(incrementCounterAction(counter))
		var iteratedActions: List[AbstractActionBuilder] = Nil

		for (i <- 1 to times)
			iteratedActions = chainActions ::: iteratedActions

		iteratedActions = expireCounterAction(counter) :: iteratedActions ::: List(initCounterAction(counter))

		logger.debug("Adding {} Iterations", times)

		doBuild(iteratedActions)
	}
}