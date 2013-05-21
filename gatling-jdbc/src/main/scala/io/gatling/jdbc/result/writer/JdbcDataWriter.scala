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
package io.gatling.jdbc.result.writer

import java.sql.{ Connection, Date => SQLDate, DriverManager, PreparedStatement, ResultSet, Statement }

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.action.system
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ GroupMessage, RequestMessage, RunMessage, ScenarioMessage, ShortScenarioDescription }
import io.gatling.core.result.writer.DataWriter
import io.gatling.jdbc.util.SQLHelper.withStatement

object JdbcDataWriter {

	implicit class ExecuteAndClearBatch(val statement: PreparedStatement) extends AnyVal {
		def executeAndClearBatch = { statement.executeBatch; statement.clearBatch; statement.getConnection.commit }
	}
}

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation to a database
 */
class JdbcDataWriter extends DataWriter with Logging {

	import JdbcDataWriter._

	/**
	 * The OutputStreamWriter used to write to db
	 */
	private val bufferSize: Int = configuration.data.jdbc.bufferSize
	private var conn: Connection = _ // TODO investigate if need 1 connection is enough
	private var runId: Int = _
	private var scenarioInsert: PreparedStatement = _
	private var groupInsert: PreparedStatement = _
	private var requestInsert: PreparedStatement = _

	private var scenarioCounter: Int = 0
	private var groupCounter: Int = 0
	private var requestCounter: Int = 0

	override def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription]) {
		conn = DriverManager.getConnection(configuration.data.jdbc.db.url, configuration.data.jdbc.db.username, configuration.data.jdbc.db.password)
		system.registerOnTermination(conn.close)

		conn.setAutoCommit(false)

		for {
			createRunRecordTable <- configuration.data.jdbc.createRunRecordTable
			createRequestRecordTable <- configuration.data.jdbc.createRequestRecordTable
			createScenarioRecord <- configuration.data.jdbc.createScenarioRecord
			createGroupRecord <- configuration.data.jdbc.createGroupRecord
		} {
			withStatement(conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) { statement =>
				statement.executeUpdate(createRunRecordTable)
				statement.executeUpdate(createRequestRecordTable)
				statement.executeUpdate(createScenarioRecord)
				statement.executeUpdate(createGroupRecord)
			}
		}

		//Insert queries for batch processing
		scenarioInsert = conn.prepareStatement("INSERT INTO ScenarioRecords (runId, scenarioName, userId, event, startDate, endDate) VALUES (?,?,?,?,?,?) ")
		system.registerOnTermination(scenarioInsert.close)
		groupInsert = conn.prepareStatement("INSERT INTO GroupRecords (runId, scenarioName, userId, entryDate, exitDate, status) VALUES (?,?,?,?,?,?) ")
		system.registerOnTermination(groupInsert.close)
		requestInsert = conn.prepareStatement("INSERT INTO RequestRecords (runId, scenario, userId, name, requestStartDate, requestEndDate, responseStartDate, responseEndDate, status, message, responseTime) VALUES (?,?,?,?,?,?,?,?,?,?,?) ")
		system.registerOnTermination(requestInsert.close)

		//Filling in run information
		withStatement(conn.prepareStatement("INSERT INTO RunRecords(runDate, simulationId, runDescription) VALUES(?,?,?)")) { runInsert =>
			runInsert.setDate(1, new SQLDate(run.runDate.toDate.getTime))
			runInsert.setString(2, run.simulationId)
			runInsert.setString(3, run.runDescription)
			runInsert.executeUpdate
			val keys: ResultSet = runInsert.getGeneratedKeys
			//Getting the runId to be dumped later on other tables.
			while (keys.next) { runId = keys.getInt(1) }
		}
	}

	override def onScenarioMessage(scenario: ScenarioMessage) {

		import scenario._
		scenarioInsert.setInt(1, runId)
		scenarioInsert.setString(2, scenarioName)
		scenarioInsert.setInt(3, userId)
		scenarioInsert.setString(4, event.name)
		scenarioInsert.setLong(5, startDate)
		scenarioInsert.setLong(6, endDate)
		scenarioInsert.addBatch

		scenarioCounter += 1

		if (scenarioCounter == bufferSize) {
			scenarioInsert.executeAndClearBatch
			scenarioCounter = 0
		}
	}

	override def onGroupMessage(group: GroupMessage) {

		import group._
		groupInsert.setInt(1, runId)
		groupInsert.setString(2, scenarioName)
		groupInsert.setInt(3, userId)
		groupInsert.setLong(4, entryDate)
		groupInsert.setLong(5, exitDate)
		groupInsert.setString(6, status.toString)
		groupInsert.addBatch

		groupCounter += 1

		if (groupCounter > bufferSize) {
			groupInsert.executeAndClearBatch
			groupCounter = 0
		}
	}

	override def onRequestMessage(request: RequestMessage) {

		import request._
		requestInsert.setInt(1, runId)
		requestInsert.setString(2, scenario)
		requestInsert.setInt(3, userId)
		requestInsert.setString(4, name)
		requestInsert.setLong(5, requestStartDate)
		requestInsert.setLong(6, requestEndDate)
		requestInsert.setLong(7, responseStartDate)
		requestInsert.setLong(8, responseEndDate)
		requestInsert.setString(9, status.toString)
		requestInsert.setString(10, message.getOrElse(null))
		requestInsert.setLong(11, responseTime)
		requestInsert.addBatch

		requestCounter += 1

		if (requestCounter > bufferSize) {
			requestInsert.executeAndClearBatch
			requestCounter = 0
		}
	}

	override def onFlushDataWriter {
		logger.info("Received flush order")
		//Flush all the batch jdbc execution
		scenarioInsert.executeAndClearBatch
		groupInsert.executeAndClearBatch
		requestInsert.executeAndClearBatch
	}
}