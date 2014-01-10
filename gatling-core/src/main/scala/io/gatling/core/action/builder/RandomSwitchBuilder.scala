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
package io.gatling.core.action.builder

import scala.annotation.tailrec
import scala.concurrent.forkjoin.ThreadLocalRandom

import com.typesafe.scalalogging.slf4j.StrictLogging

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.session.Expression
import io.gatling.core.action.Switch
import io.gatling.core.config.Protocols
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder
import io.gatling.core.validation.SuccessWrapper

class RandomSwitchBuilder(possibilities: List[(Double, ChainBuilder)], elseNext: Option[ChainBuilder]) extends ActionBuilder with StrictLogging {

	val sum = possibilities.map(_._1).sum
	require(sum <= 100, "Can't build a random switch with percentage sum > 100")
	if (sum < 100 && elseNext.isDefined)
		logger.warn("Randow switch has a 100% sum, yet a else is defined?!")

	def build(next: ActorRef, protocols: Protocols) = {

		val possibleActions = possibilities.map {
			case (percentage, possibility) =>
				val possibilityAction = possibility.build(next, protocols)
				(percentage, possibilityAction)
		}

		val elseNextActor = elseNext.map(_.build(next, protocols)).getOrElse(next)

		val nextAction: Expression[ActorRef] = _ => {

			@tailrec
			def determineNextAction(index: Double, possibilities: List[(Double, ActorRef)]): ActorRef = possibilities match {
				case Nil => elseNextActor
				case (percentage, possibleAction) :: others =>
					if (percentage >= index)
						possibleAction
					else
						determineNextAction(index - percentage, others)
			}

			determineNextAction(ThreadLocalRandom.current.nextDouble(1, 101), possibleActions).success
		}

		actor(new Switch(nextAction, next))
	}
}
