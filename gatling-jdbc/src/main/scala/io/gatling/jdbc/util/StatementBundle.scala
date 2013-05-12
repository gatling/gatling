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
package io.gatling.jdbc.util

import java.sql.Connection

import io.gatling.jdbc.statement.builder.AbstractJdbcStatementBuilder

object StatementBundle {
	def apply(name: String,builder : AbstractJdbcStatementBuilder[_],params: List[Any]) = new StatementBundle(name,builder,params)
}

class StatementBundle(val name : String,builder: AbstractJdbcStatementBuilder[_],params: List[Any]) {

	def buildStatement(connection: Connection) = {
		val statement = builder.build(connection)
		val indexes = 1 to params.length
		indexes.zip(params).map{case (index,param) => statement.setObject(index,param)}
		statement
	}
}
