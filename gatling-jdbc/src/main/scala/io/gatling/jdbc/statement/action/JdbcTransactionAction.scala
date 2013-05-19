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
package io.gatling.jdbc.statement.action

import scala.language.existentials
import io.gatling.core.session.Session
import io.gatling.core.validation.{ Failure, Success, ValidationList }
import io.gatling.jdbc.statement.action.actor.{ GetConnection, JdbcTransactionActor }
import io.gatling.jdbc.statement.builder.AbstractJdbcStatementBuilder
import io.gatling.jdbc.util.StatementBundle

import akka.actor.{ ActorRef, Props }

object JdbcTransactionAction {
	def apply(builders: List[AbstractJdbcStatementBuilder[_]],isolationLevel: Option[Int],next: ActorRef) = new JdbcTransactionAction(builders,isolationLevel,next)
}

class JdbcTransactionAction(builders: List[AbstractJdbcStatementBuilder[_]],isolationLevel: Option[Int],val next: ActorRef) extends JdbcAction {

	/**
	 * Core method executed when the Action received a Session message
	 *
	 * @param session the session of the virtual user
	 * @return Nothing
	 */
	def execute(session: Session) {
		def buildBundle(bundleData: (AbstractJdbcStatementBuilder[_],(String,List[Any]))) = {
			val (builder,(name,params)) = bundleData
			StatementBundle(name,builder,params)
		}
		val resolvedQueries = builders.map(resolveQuery(_,session)).sequence
		resolvedQueries match {
			case Success(resolvedNamesAndParams) =>
				val bundles = builders.zip(resolvedNamesAndParams).map(buildBundle(_))
				val jdbcActor = context.actorOf(Props(JdbcTransactionActor(bundles,session,next)))
				jdbcActor ! GetConnection(isolationLevel)

			case Failure(message) =>
				logger.error(message)
				next ! session
		}
	}
}
