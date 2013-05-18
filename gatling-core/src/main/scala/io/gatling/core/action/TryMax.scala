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
package io.gatling.core.action

import akka.actor.{ Actor, ActorRef, Props }
import io.gatling.core.result.message.{ OK, KO }
import io.gatling.core.session.Session
import io.gatling.core.structure.Loops.{ CounterName, SessionCounters }

class TryMax(times: Int, next: ActorRef)(implicit counterName: CounterName) extends Actor {

	var innerTryMax: ActorRef = _

	val uninitialized: Receive = {
		case loopNext: ActorRef =>
			innerTryMax = context.actorOf(Props(new InnerTryMax(times, loopNext, next)))
			context.become(initialized)
	}

	val initialized: Receive = { case m => innerTryMax forward m }

	override def receive = uninitialized
}

class InnerTryMax(times: Int, loopNext: ActorRef, val next: ActorRef)(implicit counterName: CounterName) extends Chainable {

	val interrupt: Receive = {
		case session: Session if session.status == KO =>
			if (session.counterValue < times)
				self ! session.resetStatus
			else
				next ! session.exitTryMax.exitLoop
	}

	val exitNormally: Receive = {
		case session: Session if session.status == OK && session.counterValue > 1 => next ! session.exitTryMax.exitLoop
	}

	/**
	 * Evaluates the condition and if true executes the first action of loopNext
	 * else it executes next
	 *
	 * @param session the session of the virtual user
	 */
	def execute(session: Session) {

		val initializedSession = if (!session.isSetUp) session.enterTryMax(interrupt) else session
		val incrementedSession = initializedSession.incrementLoop

		exitNormally.orElse(interrupt).applyOrElse(incrementedSession, loopNext !)
	}
}