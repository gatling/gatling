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
package io.gatling.core.result.writer

import java.sql.{ Connection, DriverManager, PreparedStatement, ResultSet, Statement }

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ GroupMessage, RequestMessage, RunMessage, ScenarioMessage, ShortScenarioDescription }

object JDBCDataWriter {

	implicit class ExecuteAndClearBatch(val statement: PreparedStatement) extends AnyVal {
		def executeAndClearBatch = { statement.executeBatch; statement.clearBatch; statement.getConnection.commit }
	}
}

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation to a database
 */
class JDBCDataWriter extends DataWriter with Logging {
	
	import JDBCDataWriter._

	/**
	 * The OutputStreamWriter used to write to db
	 */
	private val bufferCount: Int = 10
	private var conn: Connection = _
	private var statement: Statement = _
	private var runId: Int = _
	private var scenarioInsert: PreparedStatement = _
	private var groupInsert: PreparedStatement = _
	private var requestInsert: PreparedStatement = _
	private var runInsert: PreparedStatement = _

	private var scenarioCounter: Int = 0
	private var groupCounter: Int = 0
	private var requestCounter: Int = 0

	override def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription]) {
		conn = DriverManager.getConnection(configuration.data.jdbc.db.url, configuration.data.jdbc.db.username, configuration.data.jdbc.db.password)
		conn.setAutoCommit(false)
		statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)

		//Create tables if it doesnt exist
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `RunRecords` ( `id` INT NOT NULL AUTO_INCREMENT , `runDate` DATETIME NULL , `simulationId` VARCHAR(45) NULL , `runDescription` VARCHAR(45) NULL , PRIMARY KEY (`id`) )")
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `RequestRecords` (`id` int(11) NOT NULL AUTO_INCREMENT, `runId` int(11) DEFAULT NULL, `scenario` varchar(45) DEFAULT NULL, `userId` int(11) DEFAULT NULL, `name` varchar(50) DEFAULT NULL, `requestStartDate` varchar(45) DEFAULT NULL, `requestEndDate` varchar(45) DEFAULT NULL, `responseStartDate` varchar(45) DEFAULT NULL, `responseEndDate` varchar(45) DEFAULT NULL, `status` varchar(45) DEFAULT NULL, `message` varchar(4500) DEFAULT NULL, `responseTime` int(11) DEFAULT NULL, PRIMARY KEY (`id`) )")
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ScenarioRecords` (`id` int(11) NOT NULL AUTO_INCREMENT, `runId` int(11) DEFAULT NULL, `scenarioName` varchar(45) DEFAULT NULL, `userId` int(11) DEFAULT NULL, `event` varchar(50) DEFAULT NULL, `startDate` varchar(45) DEFAULT NULL, `endDate` varchar(45) DEFAULT NULL, PRIMARY KEY (`id`) )")
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `GroupRecords` (`id` int(11) NOT NULL AUTO_INCREMENT, `runId` int(11) DEFAULT NULL, `scenarioName` varchar(45) DEFAULT NULL, `userId` int(11) DEFAULT NULL, `entryDate` varchar(50) DEFAULT NULL, `exitDate` varchar(45) DEFAULT NULL,`status` varchar(45) DEFAULT NULL, PRIMARY KEY (`id`) )")

		//Insert queries for batch processing
		scenarioInsert = conn.prepareStatement("INSERT INTO ScenarioRecords (runId, scenarioName, userId, event, startDate, endDate) VALUES (?,?,?,?,?,?) ")
		groupInsert = conn.prepareStatement("INSERT INTO GroupRecords (runId, scenarioName, userId, entryDate, exitDate, status) VALUES (?,?,?,?,?,?) ")
		requestInsert = conn.prepareStatement("INSERT INTO RequestRecords (runId, scenario, userId, name, requestStartDate, requestEndDate, responseStartDate, responseEndDate, status, message, responseTime) VALUES (?,?,?,?,?,?,?,?,?,?,?) ")

		//Filling in run information
		runInsert = conn.prepareStatement("INSERT INTO RunRecords(runDate, simulationId, runDescription) VALUES(?,?,?)")
		runInsert.setDate(1, new java.sql.Date(run.runDate.toDate.getTime))
		runInsert.setString(2, run.simulationId)
		runInsert.setString(3, run.runDescription)
		runInsert.executeUpdate
		val keys: ResultSet = runInsert.getGeneratedKeys
		//Getting the runId to be dumped later on other tables.
		while (keys.next) { runId = keys.getInt(1) }
	}

	override def onScenarioMessage(scenario: ScenarioMessage) {
		//Do a batch update on reaching buffer count
		if (scenarioCounter > bufferCount) { scenarioInsert.executeAndClearBatch }
		import scenario._
		scenarioInsert.setInt(1, runId)
		scenarioInsert.setString(2, scenarioName)
		scenarioInsert.setInt(3, userId)
		scenarioInsert.setString(4, event.name)
		scenarioInsert.setString(5, startDate.toString)
		scenarioInsert.setString(6, endDate.toString)
		scenarioInsert.addBatch

		scenarioCounter = (scenarioCounter + 1) % bufferCount
	}

	override def onGroupMessage(group: GroupMessage) {
		//Do a batch update on reaching buffer count
		if (groupCounter > bufferCount) { groupInsert.executeAndClearBatch }
		import group._
		groupInsert.setInt(1, runId)
		groupInsert.setString(2, scenarioName)
		groupInsert.setInt(3, userId)
		groupInsert.setString(4, entryDate.toString)
		groupInsert.setString(5, exitDate.toString)
		groupInsert.setString(6, status.toString)
		groupInsert.addBatch

		groupCounter = (groupCounter + 1) % bufferCount
	}

	override def onRequestMessage(request: RequestMessage) {
		//Do a batch update on reaching buffer count
		if (requestCounter > bufferCount) { requestInsert.executeAndClearBatch }
		import request._
		requestInsert.setInt(1, runId)
		requestInsert.setString(2, scenario)
		requestInsert.setInt(3, userId)
		requestInsert.setString(4, name)
		requestInsert.setString(5, requestStartDate.toString)
		requestInsert.setString(6, requestEndDate.toString)
		requestInsert.setString(7, responseStartDate.toString)
		requestInsert.setString(8, responseEndDate.toString)
		requestInsert.setString(9, status.toString)
		requestInsert.setString(10, message.toString)
		requestInsert.setInt(11, responseTime.toInt)
		requestInsert.addBatch

		requestCounter = (requestCounter + 1) % bufferCount
	}

	override def onFlushDataWriter {
		logger.info("Received flush order")
		//Flush all the batch jdbc execution
		scenarioInsert.executeAndClearBatch
		groupInsert.executeAndClearBatch
		requestInsert.executeAndClearBatch

		//Closing all the connections
		requestInsert.close
		scenarioInsert.close
		groupInsert.close
		statement.close
		conn.close
	}
}