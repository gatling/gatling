package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.result.message.RecordEvent.END
import com.excilys.ebi.gatling.core.result.terminator.Terminator
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.{ ActorRef, Props }

object UserEnd {

	val END = system.actorOf(Props(new UserEnd))
}

class UserEnd extends Action {

	def next: ActorRef = null // FIXME

	def execute(session: Session) {

		DataWriter.user(session.scenarioName, session.userId, END)
		info("End user #" + session.userId)

		Terminator.endUser
	}
}