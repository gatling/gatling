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
package com.excilys.ebi.gatling.jdbc.statement.action

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK, RequestStatus }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.jdbc.statement.action.JdbcHandler._
import java.sql.{ResultSet, PreparedStatement}
import akka.dispatch.{Await,Future}
import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session
import java.util.concurrent.TimeoutException
import akka.util.duration.intToDurationInt
import akka.actor.ActorRef


object JdbcHandler {

	// FIXME : tune jdbc-dispatcher in application.conf
	implicit val executionContext = system.dispatchers.lookup("jdbc-dispatcher")

	implicit def ResultSet2RowIterator(resultSet: ResultSet): RowIterator = new RowIterator(resultSet)

	def apply(statementName: String,statement: PreparedStatement,session: Session,next: ActorRef) = new JdbcHandler(statementName,statement,session,next)
}

class JdbcHandler(statementName: String,statement: PreparedStatement,session: Session,next: ActorRef) extends Logging {

	var executionStartDate: Int = _
	var statementExecutionStartDate: Long = _
	var statementExecutionEndDate: Long = _
	var executionEndDate: Long = _

	val executionFuture = Future {
		statement.execute()
		nowMillis
	}

	val processingFuture = Future {
		if(statement.getUpdateCount != 1) processResultSet
		nowMillis
	}

	def execute = {
		// TODO : need to handle executionStartDate : do it here, or do it in jdbcStatementAction ?
		use(statement) ( statement => {
			val timeout = configuration.jdbc.statementTimeoutInMs milliseconds
			try {
				statementExecutionEndDate = Await.result(executionFuture,timeout)
				executionEndDate = Await.result(processingFuture,timeout)
				logStatement(OK)
			} catch {
				case te: TimeoutException =>
					executionEndDate = nowMillis
					logStatement(KO,Some(te.getMessage))
					executeNext(session.setFailed)
				case e: Exception =>
					logStatement(KO,Some("JdbcHandler timed out"))
					executeNext(session.setFailed)
			}
		})
	}

	def executeNext(newSession: Session) = next ! newSession.increaseTimeShift(nowMillis - executionEndDate)

	private def processResultSet = use(statement.getResultSet) { _.foreach(logRow(_))}

	private def logRow(rowContents: Map[Int,AnyRef]) {
		val formatted = rowContents.map{case (columnIndex,content) => columnIndex + "=" + content}.mkString(",")
		debug("Row contents : " + formatted)
	}

	private def logStatement(status: RequestStatus,errorMessage: Option[String] = None) {
		DataWriter.logRequest(session.scenarioName,session.userId,statementName,executionStartDate,
			statementExecutionStartDate,statementExecutionEndDate,executionEndDate,status,errorMessage)
	}
}
