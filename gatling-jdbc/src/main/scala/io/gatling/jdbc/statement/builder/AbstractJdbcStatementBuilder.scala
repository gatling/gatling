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
package io.gatling.jdbc.statement.builder

import java.sql.Connection

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.ValidationList
import io.gatling.jdbc.statement.{ CALL, QUERY, StatementType }
import io.gatling.jdbc.statement.action.builder.JdbcStatementActionBuilder

case class JdbcAttributes(
	statementName: Expression[String],
	statement: String,
	statementType: StatementType,
	params: List[Expression[Any]])

abstract class AbstractJdbcStatementBuilder[B <: AbstractJdbcStatementBuilder[B]](jdbcAttributes: JdbcAttributes) {

	private[jdbc] def newInstance(jdbcAttributes: JdbcAttributes): B

	def bind(value: Expression[Any]) = newInstance(jdbcAttributes.copy(params = value :: jdbcAttributes.params))

	private[gatling] def toActionBuilder = JdbcStatementActionBuilder(this)

	private[jdbc] def statementName = jdbcAttributes.statementName

	private[jdbc] def build(connection: Connection) = createStatement(connection)

	private def createStatement(connection: Connection) = jdbcAttributes.statementType match {
		case CALL => connection.prepareCall(jdbcAttributes.statement)
		case QUERY => connection.prepareStatement(jdbcAttributes.statement)
	}

	private[jdbc] def resolveParams(session: Session) = jdbcAttributes.params.reverse.map(_(session)).sequence
}

