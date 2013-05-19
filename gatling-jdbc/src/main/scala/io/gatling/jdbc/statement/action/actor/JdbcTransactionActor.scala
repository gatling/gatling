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

import java.lang.System.nanoTime

import java.sql.PreparedStatement

import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.computeTimeMillisFromNanos
import io.gatling.jdbc.util.StatementBundle

import akka.actor.ActorRef

object JdbcTransactionActor {

	def apply(bundles: Seq[StatementBundle],session: Session,next: ActorRef) = new JdbcTransactionActor(bundles,session,next)

}

class JdbcTransactionActor(bundles: Seq[StatementBundle],session: Session,next: ActorRef) extends JdbcActor(session,next) {

	var statements = bundles.toList

	def onTimeout = failTransaction("JdbcTransactionActor timed out")

	def onConnectionReady {
		connection.setAutoCommit(false)
		doNext
	}

	def doNext  {
		val nextStatement = statements.head
		currentStatementName = nextStatement.name
		self ! BuildStatement(nextStatement,connection)
	}

	def onStatementBuilt(newStatement: PreparedStatement) {
		statementExecutionStartDate = computeTimeMillisFromNanos(nanoTime)
		self ! ExecuteStatement(newStatement)
	}

	def onStatementExecuted(statement: PreparedStatement,hasResultSet: Boolean) {
		statementExecutionStartDate = computeTimeMillisFromNanos(nanoTime)
		self ! ProcessResultSet(statement,hasResultSet)
	}

	def onResultSetProcessed {
		executionEndDate = computeTimeMillisFromNanos(nanoTime)
		logCurrentStatement(OK)
		statements = statements.tail
		statements match {
			case Nil => commit
			case _ => doNext
		}
	}

	def commit {
		try {
			connection.commit
			executeNext(session)
		} catch {
			case e : Exception =>
				try connection.rollback catch { case e: Exception => () }
				failTransaction(e.getMessage)
		}
	}

	def onExceptionCaught(errorMessage: String) = failTransaction(errorMessage)

	def failTransaction(message: String) {
		logCurrentStatement(KO,Some(message))
		failRemainingStatements
		executeNext(session.markAsFailed)
	}

	def failRemainingStatements {
		val failingQueryName = statements.head.name
		statements.tail.foreach(bundle => logStatement(bundle.name,KO,Some(s"Transaction failed because '$failingQueryName' failed ")))
	}
}