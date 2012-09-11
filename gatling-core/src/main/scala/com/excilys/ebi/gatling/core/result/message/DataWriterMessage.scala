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
package com.excilys.ebi.gatling.core.result.message

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.result.message.RecordType.{ ACTION, RUN }
import com.excilys.ebi.gatling.core.util.DateHelper.{ toHumanDate, toTimestamp }

sealed trait DataWriterMessage

case class ShortScenarioDescription(name: String, nbUsers: Int)

/**
 * This case class is to be sent to the logging actor, it contains all the information
 * required for its initialization
 *
 * @param runRecord the data on the simulation run
 * @param totalUsersCount the number of total users
 * @param encoding the file encoding
 */
case class InitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) extends DataWriterMessage

case object FlushDataWriter extends DataWriterMessage

/**
 * This case class is to be sent to the logging actor, it contains all the information
 * required for statistics generation after the simulation has run
 *
 * @param scenarioName the name of the current scenario
 * @param userId the id of the current user being simulated
 * @param requestName the name of the action that was made
 * @param executionStartDate the date on which the action was made
 * @param executionEndDate the date on which the action was completed
 * @param requestSendingEndDate the date on which the request was finished being sent
 * @param responseReceivingStartDate the date on which the response was started being received
 * @param requestStatus the status of the action
 * @param requestMessage the message of the action on completion
 * @param extraInfo information about the request and response extracted via a user-defined function
 */
case class RequestRecord(
	scenarioName: String,
	userId: Int,
	requestName: String,
	executionStartDate: Long,
	executionEndDate: Long,
	requestSendingEndDate: Long,
	responseReceivingStartDate: Long,
	requestStatus: RequestStatus.RequestStatus,
	requestMessage: Option[String] = None,
	extraInfo: List[String] = Nil) extends DataWriterMessage {
	val recordType = ACTION
	def latency = responseReceivingStartDate - requestSendingEndDate
	def responseTime = executionEndDate - executionStartDate
}

case class RunRecord(runDate: DateTime, simulationId: String, runDescription: String) extends DataWriterMessage {
	val recordType = RUN
	val timestamp =  toTimestamp(runDate)
	def runId = simulationId + "-" + timestamp
	def readableRunDate = toHumanDate(runDate)
}