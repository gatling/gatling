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

import java.sql.{ Connection, PreparedStatement }

import scala.math.max

import io.gatling.core.action.BaseActor
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{RequestMessage, KO, Status}
import io.gatling.core.result.writer.DataWriter
import io.gatling.core.session.Session
import io.gatling.core.util.IOHelper.withResultSet
import io.gatling.core.util.TimeHelper.nowMillis

import akka.actor.{ ActorRef, ReceiveTimeout }
import scala.concurrent.duration.DurationInt

abstract class JdbcActor(session: Session,next: ActorRef) extends BaseActor {

	var currentStatementName: String = _
	var connection: Connection = _

	var executionStartDate = nowMillis
	var statementExecutionStartDate = 0L
	var statementExecutionEndDate = 0L
	var executionEndDate = 0L

	resetTimeout

	def onConnectionReady

	def onStatementBuilt(newStatement: PreparedStatement)

	def onStatementExecuted(statement: PreparedStatement,hasResultSet: Boolean)

	def onResultSetProcessed

	def onExceptionCaught(errorMessage: String)

	def onTimeout

	def receive = resultsMessages orElse jdbcOperations

	def resultsMessages: Receive = {
		case ConnectionReady(newConnection) =>
			resetTimeout
			connection = newConnection
			onConnectionReady

		case StatementBuilt(statement) =>
			resetTimeout
			onStatementBuilt(statement)

		case StatementExecuted(statement,hasResultSet) =>
			resetTimeout
			onStatementExecuted(statement,hasResultSet)

		case ResultSetProcessed =>
			resetTimeout
			onResultSetProcessed

		case CaughtException(errorMessage) => onExceptionCaught(errorMessage)

		case ReceiveTimeout => onTimeout
	}

	def jdbcOperations: Receive = {
		case GetConnection(isolationLevel) => self ! answer(ConnectionReady(setupConnection(isolationLevel)))

		case BuildStatement(bundle,connection) => self ! answer(StatementBuilt(bundle.buildStatement(connection)))

		case ExecuteStatement(statement) => self ! answer(StatementExecuted(statement,statement.execute))

		case ProcessResultSet(statement,hasResultSet) => self ! answer { if(hasResultSet) processResultSet(statement); ResultSetProcessed }
	}

	def answer[T <: JdbcActorMessage](message: => T) = try message catch { case e: Exception => CaughtException(e.getMessage) }

	def setupConnection(isolationLevel: Option[Int]) = {
		val connection = ConnectionFactory.getConnection
		if (isolationLevel.isDefined) connection.setTransactionIsolation(isolationLevel.get)
		connection
	}

	def processResultSet(statement: PreparedStatement) {
			withResultSet(statement.getResultSet) { resultSet =>
				var count = 0
				while (resultSet.next) {
				count = count + 1
			}
			statement.close
			}
	}

	def closeStatement(statement: PreparedStatement) = if (statement != null) statement.close

	def executeNext(newSession: Session) {
		if(connection != null) connection.close
		next ! newSession.increaseTimeShift(nowMillis - executionEndDate)
		context.stop(self)
	}

	def resetTimeout = context.setReceiveTimeout(configuration.jdbc.statementTimeoutInMs milliseconds)

	def logCurrentStatement(status: Status,errorMessage: Option[String] = None) = logStatement(currentStatementName,status,errorMessage)

	def logStatement(statementName: String,status: Status, errorMessage: Option[String] = None) {
		// time measurement is imprecise due to multi-core nature
		// ensure statement execution doesn't start before starting
		statementExecutionStartDate = max(statementExecutionStartDate,executionStartDate)
		// ensure statement execution doesn't end before it starts
		statementExecutionEndDate = max(statementExecutionEndDate,statementExecutionStartDate)
		// ensure execution doesn't end before statement execution ends
		executionEndDate = max(executionEndDate,statementExecutionEndDate)
		// Log request
		if (status == KO) logger.warn(s"Statement '$statementName' failed : ${errorMessage.getOrElse("")}")
		DataWriter.tell(RequestMessage(session.scenarioName,session.userId,session.groupStack,statementName,executionStartDate,
			statementExecutionStartDate,statementExecutionEndDate,executionEndDate,status,errorMessage))
	}
}
