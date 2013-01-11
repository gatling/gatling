package com.excilys.ebi.gatling.core.action

import akka.actor.ActorRef
import com.excilys.ebi.gatling.core.session.Session

trait Chainable extends Action {

	def next: ActorRef

	override def preRestart(reason: Throwable, message: Option[Any]) {
		error("Action " + this + " crashed, forwarding user to next one", reason)
		message match {
			case Some(session: Session) => next ! session.setFailed
			case _ =>
		}
	}
}