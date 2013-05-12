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
package io.gatling.jdbc

import io.gatling.core.session.Expression
import io.gatling.jdbc.config.JdbcProtocolBuilder
import io.gatling.jdbc.statement.builder.{ AbstractJdbcStatementBuilder, JdbcStatementBaseBuilder }
import io.gatling.jdbc.statement.action.builder.JdbcTransactionActionBuilder
import io.gatling.jdbc.feeder.database.JdbcFeederSource

object Predef {

	val READ_COMMITTED = "READ_COMMITTED"
	val REPEATABLE_READ = "REPEATABLE_READ"
	val READ_UNCOMMITTED = "READ_UNCOMMITTED"
	val SERIALIZABLE = "SERIALIZABLE"

	implicit def jdbcProtocolConfigurationBuilder2JdbcProtocolConfiguration(builder: JdbcProtocolBuilder) = builder.build
	implicit def statementBuilder2ActionBuilder(statementBuilder: AbstractJdbcStatementBuilder[_]) = statementBuilder.toActionBuilder

	def jdbcConfig = JdbcProtocolBuilder.jdbcConfig
	def sql(statementName: Expression[String]) = JdbcStatementBaseBuilder.sql(statementName)
	def transaction(queries: AbstractJdbcStatementBuilder[_]*) = JdbcTransactionActionBuilder(queries)

	def jdbcFeeder(url: String, username: String, password: String, sql: String) = JdbcFeederSource(url, username, password, sql)

}