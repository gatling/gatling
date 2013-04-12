/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.core.result.message

import org.joda.time.DateTime

import io.gatling.core.util.DateHelper.RichDateTime

sealed trait DataWriterMessage

case class ShortScenarioDescription(name: String, nbUsers: Int)

case class Init(runMessage: RunMessage, scenarios: Seq[ShortScenarioDescription]) extends DataWriterMessage

case object Flush extends DataWriterMessage

case class RequestMessage(
	scenario: String,
	userId: Int,
	groupStack: List[GroupStackEntry],
	name: String,
	requestStartDate: Long,
	requestEndDate: Long,
	responseStartDate: Long,
	responseEndDate: Long,
	status: Status,
	message: Option[String] = None,
	extraInfo: List[Any] = Nil) extends DataWriterMessage {
	val recordType = RequestMessageType
	def responseTime = responseEndDate - requestStartDate
}

case class RunMessage(runDate: DateTime, simulationId: String, runDescription: String) extends DataWriterMessage {
	val recordType = RunMessageType
	val timestamp = runDate.toTimestamp
	def runId = simulationId + "-" + runDate.toTimestamp
}

case class ScenarioMessage(scenarioName: String, userId: Int, event: MessageEvent, startDate: Long, endDate: Long) extends DataWriterMessage {
	val recordType = ScenarioMessageType
}

case class GroupMessage(scenarioName: String, groupStack: List[GroupStackEntry], userId: Int, entryDate: Long, exitDate: Long, status: Status) extends DataWriterMessage {
	val recordType = GroupMessageType
}

