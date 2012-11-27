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

import com.excilys.ebi.gatling.core.session.{Session, EvaluatableString}
import akka.actor.ActorRef
import com.excilys.ebi.gatling.jdbc.statement.builder.AbstractJdbcStatementBuilder
import com.excilys.ebi.gatling.core.action.{Bypass, Action}
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.jdbc.util.ConnectionFactory

object JdbcStatementAction {

	def apply(statementName: EvaluatableString, statementBuilder: AbstractJdbcStatementBuilder[_],isolationLevel: Option[Int],next: ActorRef) =
		new JdbcStatementAction(statementName,statementBuilder,isolationLevel,next)
}

class JdbcStatementAction(statementName: EvaluatableString, statementBuilder: AbstractJdbcStatementBuilder[_],isolationLevel: Option[Int],val next: ActorRef) extends Action with Bypass {

	/**
	 * Core method executed when the Action received a Session message
	 *
	 * @param session the session of the virtual user
	 * @return Nothing
	 */
	def execute(session: Session) {
		// TODO : fail statement if error (maybe move it to JdbcHandler, or move JdbcHandler here ?
		use(ConnectionFactory.getConnection) {	connection =>
			connection.setTransactionIsolation(isolationLevel.getOrElse(connection.getTransactionIsolation))
			val statement = statementBuilder.build(connection)
			JdbcHandler(statement).execute
		}

	}
}
