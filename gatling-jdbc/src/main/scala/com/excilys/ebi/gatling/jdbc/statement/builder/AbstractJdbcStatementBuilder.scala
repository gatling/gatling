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
package com.excilys.ebi.gatling.jdbc.statement.builder

import com.excilys.ebi.gatling.core.session.{EvaluatableString,EvaluatableStringToAny}
import com.excilys.ebi.gatling.jdbc.statement.StatementType.{StatementType,CALL,QUERY}
import com.excilys.ebi.gatling.jdbc.statement.action.JdbcStatementActionBuilder
import java.sql.Connection

case class JdbcAttributes(
	statementName: EvaluatableString,
	statement: String,
	statementType: StatementType,
	params: List[Any],
	isolationLevel: Option[Int])

abstract class AbstractJdbcStatementBuilder[B <: AbstractJdbcStatementBuilder[B]](jdbcAttributes: JdbcAttributes) {

	private[jdbc] def newInstance(jdbcAttributes: JdbcAttributes) : B

	def isolation(isolationLevel: Int) = newInstance(jdbcAttributes.copy(isolationLevel = Some(isolationLevel)))

	def bind(value: EvaluatableStringToAny) = throw new UnsupportedOperationException("Not yet implemented.")

	def bind(value: Any) = newInstance(jdbcAttributes.copy(params = value :: jdbcAttributes.params))

	private[gatling] def toActionBuilder = JdbcStatementActionBuilder(jdbcAttributes.statementName,this,jdbcAttributes.isolationLevel)

	private[jdbc] def build(connection: Connection) = createStatement(connection)

	private def createStatement(connection: Connection) = jdbcAttributes.statementType match {
		case CALL => connection.prepareCall(jdbcAttributes.statement)
		case QUERY => connection.prepareStatement(jdbcAttributes.statement)
	}
}

