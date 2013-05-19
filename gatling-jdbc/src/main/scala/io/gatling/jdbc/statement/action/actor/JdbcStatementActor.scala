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
package io.gatling.jdbc.statement.action.actor

import java.lang.System.nanoTime
import java.sql.PreparedStatement

import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.computeTimeMillisFromNanos
import io.gatling.jdbc.util.StatementBundle

import akka.actor.ActorRef

object JdbcStatementActor {

	def apply(bundle: StatementBundle,session: Session,next: ActorRef) = new JdbcStatementActor(bundle,session,next)

}

class JdbcStatementActor(bundle: StatementBundle,session: Session,next: ActorRef) extends JdbcActor(session,next) {

	currentStatementName = bundle.name

	def onTimeout = failStatement("JdbcStatementActor timed out")

	def onConnectionReady = self ! BuildStatement(bundle,connection)

	def onStatementBuilt(newStatement: PreparedStatement) {
		statementExecutionStartDate = computeTimeMillisFromNanos(nanoTime)
		self ! ExecuteStatement(newStatement)
	}

	def onStatementExecuted(statement: PreparedStatement,hasResultSet: Boolean) {
		statementExecutionEndDate = computeTimeMillisFromNanos(nanoTime)
		self ! ProcessResultSet(statement,hasResultSet)
	}

	def onResultSetProcessed {
		executionEndDate = computeTimeMillisFromNanos(nanoTime)
		logCurrentStatement(OK)
		executeNext(session)
	}

	def onExceptionCaught(errorMessage: String) = failStatement(errorMessage)

	def failStatement(message: String) {
		logCurrentStatement(KO,Some(message))
		executeNext(session.markAsFailed)
	}
}
