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
package com.excilys.ebi.gatling.core.action.builder

import scala.annotation.tailrec

import org.apache.commons.math3.random.{ RandomData, RandomDataImpl }

import com.excilys.ebi.gatling.core.action.{ SwitchAction, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.structure.ChainBuilder

import akka.actor.{ ActorRef, Props }

object RandomSwitchBuilder {

	private val randomData: RandomData = new RandomDataImpl

	def apply(possibilities: List[(Int, ChainBuilder)]) = new RandomSwitchBuilder(possibilities, null)
}

class RandomSwitchBuilder(possibilities: List[(Int, ChainBuilder)], next: ActorRef) extends ActionBuilder {

	def withNext(next: ActorRef) = new RandomSwitchBuilder(possibilities, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {

		if (possibilities.map(_._1).sum > 100) throw new IllegalArgumentException("Can't build a random switch with percentage sum > 100")

		val possibleActions = possibilities.map {
			case (percentage, possibility) =>
				val possibilityAction = possibility.withNext(next).build(protocolConfigurationRegistry)
				(percentage, possibilityAction)
		}

		val strategy = () => {

			@tailrec
			def determineNextAction(index: Int, possibilities: List[(Int, ActorRef)]): ActorRef = possibilities match {
				case Nil => next
				case (percentage, possibleAction) :: others =>
					if (percentage >= index)
						possibleAction
					else
						determineNextAction(index - percentage, others)
			}

			determineNextAction(RandomSwitchBuilder.randomData.nextInt(1, 100), possibleActions)
		}

		system.actorOf(Props(new SwitchAction(strategy, next)))
	}
}