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
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.jdbc.statement.action.JdbcHandler._
import java.sql.{ResultSet, PreparedStatement}
import akka.dispatch.Future
import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session

object JdbcHandler {

	// FIXME : tune jdbc-dispatcher in application.conf
	implicit val executionContext = system.dispatchers.lookup("jdbc-dispatcher")

	implicit def ResultSet2RowIterator(resultSet: ResultSet): RowIterator = new RowIterator(resultSet)

	def apply(statementName: String,statement: PreparedStatement,session: Session) = new JdbcHandler(statementName,statement,session)
}

class JdbcHandler(statementName: String,statement: PreparedStatement,session: Session) extends Logging {

	def execute = {
		use(statement) { statement =>
			// TODO : timestamp and everything else needed for the log
			Future(statement.execute).map(if(_) processResultSet).onComplete {
				case Left(t) => logStatement(KO,Some(t.getMessage))
				case Right(_) => logStatement(OK)
			}
		}
	}

	private def processResultSet = use(statement.getResultSet) { _.foreach(logRow(_))}

	private def logRow(rowContents: Map[Int,AnyRef]) {
		val formatted = rowContents.map{case (columnIndex,content) => columnIndex + "=" + content}.mkString(",")
		debug("Row contents : " + formatted)
	}

	private def logStatement(status: RequestStatus,errorMessage: Option[String] = None) {
		// FIXME : replace the dummy timestamps when they're properly implemented
		DataWriter.logRequest(session.scenarioName,session.userId,statementName,0,0,0,0,status,errorMessage)
	}
}
