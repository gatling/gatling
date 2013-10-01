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

import akka.actor.ActorRef
import io.gatling.core.akka.BaseActor
import io.gatling.core.session.Session
import io.gatling.core.validation.{ Failure, Validation }

/**
 * Top level abstraction in charge or executing concrete actions along a scenario, for example sending an HTTP request.
 * It is implemented as an Akka Actor that receives Session messages.
 */
trait Action extends BaseActor {

	def receive = {
		case session: Session => execute(session)
	}

	/**
	 * Core method executed when the Action received a Session message
	 *
	 * @param session the session of the virtual user
	 * @return Nothing
	 */
	def execute(session: Session)
}

trait Chainable extends Action {

	def next: ActorRef

	override def preRestart(reason: Throwable, message: Option[Any]) {
		logger.error(s"Action $this crashed on message $message, forwarding user to the next one", reason)
		message.foreach {
			case session: Session => next ! session.markAsFailed
			case _ =>
		}
	}
}

trait Interruptable extends Chainable {

	val interrupt: PartialFunction[Any, Unit] = {
		case session: Session if (!session.interruptStack.isEmpty) => (session.interruptStack.reduceLeft(_ orElse _) orElse super.receive)(session)
	}

	abstract override def receive = interrupt orElse super.receive
}

/**
 * Action that handles failures gracefully by returning a Validation
 */
trait Failable { self: Chainable =>

	def execute(session: Session) {
		executeOrFail(session) match {
			case Failure(message) =>
				logger.error(message)
				next ! session.markAsFailed
			case _ =>
		}
	}

	def executeOrFail(session: Session): Validation[_]
}
