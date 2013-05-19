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
import io.gatling.jdbc.util.StatementBundle

sealed trait JdbcActorMessage

case class ConnectionReady(connection: Connection) extends JdbcActorMessage
case class StatementBuilt(statement: PreparedStatement) extends JdbcActorMessage
case class StatementExecuted(statement: PreparedStatement,hasResultSet: Boolean) extends JdbcActorMessage
case object ResultSetProcessed extends JdbcActorMessage
case class CaughtException(errorMessage: String) extends JdbcActorMessage
case class GetConnection(isolationLevel: Option[Int]) extends JdbcActorMessage
case class BuildStatement(bundle: StatementBundle,connection: Connection) extends JdbcActorMessage
case class ExecuteStatement(statement: PreparedStatement) extends JdbcActorMessage
case class ProcessResultSet(statement: PreparedStatement,hasResultSet: Boolean) extends JdbcActorMessage