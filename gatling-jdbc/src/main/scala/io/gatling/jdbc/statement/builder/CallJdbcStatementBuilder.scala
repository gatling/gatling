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

import io.gatling.core.session.Expression
import io.gatling.jdbc.statement.CALL

object CallJdbcStatementBuilder {

	def apply(statementName: Expression[String],statement: String) = new CallJdbcStatementBuilder(JdbcAttributes(statementName,statement,CALL,Nil))
}

class CallJdbcStatementBuilder(jdbcAttributes: JdbcAttributes) extends AbstractJdbcStatementBuilder[CallJdbcStatementBuilder](jdbcAttributes) {

	private[jdbc] def newInstance(jdbcAttributes: JdbcAttributes) = new CallJdbcStatementBuilder(jdbcAttributes)
}
