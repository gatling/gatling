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

class TryMax(times: Int, counterName: String, next: ActorRef) extends Actor {

	var innerTryMax: ActorRef = _

	val uninitialized: Receive = {
		case loopNext: ActorRef =>
			innerTryMax = context.actorOf(Props(new InnerTryMax(times, loopNext, counterName, next)))
			context.become(initialized)
	}

	val initialized: Receive = { case m => innerTryMax forward m }

	override def receive = uninitialized
}

class InnerTryMax(times: Int, loopNext: ActorRef, counterName: String, val next: ActorRef) extends Chainable {

	val interrupt: PartialFunction[Session, Unit] = {
		case session if session.statusStack.head == KO =>
			if (session.loopCounterValue(counterName) < times)
				self ! session
			else
				next ! session.exitTryMax.exitLoop
	}

	/**
	 * Evaluates the condition and if true executes the first action of loopNext
	 * else it executes next
	 *
	 * @param session the session of the virtual user
	 */
	def execute(session: Session) {

		val initializedSession = if (!session.contains(counterName)) session.enterTryMax(interrupt) else session
		val incrementedSession = initializedSession.incrementLoop(counterName)

		val counterValue = incrementedSession.loopCounterValue(counterName)
		val status = incrementedSession.statusStack.head

		if ((status == OK && counterValue > 0) || (status == KO && counterValue >= times))
			next ! incrementedSession.exitTryMax.exitLoop // succeed or exit on exceed
		else
			loopNext ! incrementedSession.resetStatus // loop
	}
}