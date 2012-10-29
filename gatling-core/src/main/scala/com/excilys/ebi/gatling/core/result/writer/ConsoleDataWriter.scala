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
package com.excilys.ebi.gatling.core.result.writer

import java.lang.System.currentTimeMillis
import java.util.{ HashMap => JHashMap }

import scala.collection.mutable.{ HashMap, LinkedHashMap }

import com.excilys.ebi.gatling.core.result.{ Group, RequestPath }
import com.excilys.ebi.gatling.core.result.message.{ RunRecord, ScenarioRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.message.GroupRecord
import com.excilys.ebi.gatling.core.result.message.RecordSubType.{ END, START }
import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK }

import grizzled.slf4j.Logging

class UserCounters(val totalCount: Int) {

	private var _runningCount: Int = 0
	private var _doneCount: Int = 0

	def runningCount = _runningCount
	def doneCount = _doneCount

	def userStart { _runningCount += 1 }
	def userDone { _runningCount -= 1; _doneCount += 1 }
	def waitingCount = totalCount - _runningCount - _doneCount
}

case class RequestCounters(var successfulCount: Int, var failedCount: Int)

class ConsoleDataWriter extends DataWriter with Logging {

	private var startUpTime = 0L
	private var lastDisplayTime = 0L

	private val usersCounters = new HashMap[String, UserCounters]
	private val groupStack = new JHashMap[(String, Int), Option[Group]]
	private val requestsCounters = new LinkedHashMap[String, RequestCounters]

	private val displayPeriod = 5 * 1000

	private var complete = false

	def display(force: Boolean) {
		val now = currentTimeMillis
		if (force || (now - lastDisplayTime > displayPeriod)) {
			lastDisplayTime = now
			val timeSinceStartUpInSec = (now - startUpTime) / 1000

			val summary = ConsoleSummary(timeSinceStartUpInSec, usersCounters, requestsCounters)
			complete = summary.complete
			println(summary)
		}
	}

	override def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {

		startUpTime = currentTimeMillis
		lastDisplayTime = currentTimeMillis

		usersCounters.clear
		scenarios.foreach(scenario => usersCounters.put(scenario.name, new UserCounters(scenario.nbUsers)))
		requestsCounters.clear
	}

	override def onScenarioRecord(scenarioRecord: ScenarioRecord) {
		scenarioRecord.recordSubType match {
			case START =>
				usersCounters
					.get(scenarioRecord.scenarioName)
					.map(_.userStart)
					.getOrElse(error("Internal error, scenario '%s' has not been correctly initialized" format scenarioRecord.scenarioName))
				updateCurrentGroup(scenarioRecord.scenarioName, scenarioRecord.userId, _ => None)

			case END =>
				usersCounters
					.get(scenarioRecord.scenarioName)
					.map(_.userDone)
					.getOrElse(error("Internal error, scenario '%s' has not been correctly initialized" format scenarioRecord.scenarioName))
				groupStack.remove((scenarioRecord.scenarioName, scenarioRecord.userId))
		}
	}

	override def onGroupRecord(groupRecord: GroupRecord) {
		groupRecord.recordSubType match {
			case START =>
				updateCurrentGroup(groupRecord.scenarioName, groupRecord.userId, current => Some(Group(groupRecord.groupName.get, current)))

			case END =>
				updateCurrentGroup(groupRecord.scenarioName, groupRecord.userId, current => current.flatMap(_.parent))
		}
	}

	override def onRequestRecord(requestRecord: RequestRecord) {

		val requestCounters = requestsCounters.getOrElseUpdate(RequestPath.path(requestRecord.requestName, groupStack.get((requestRecord.scenarioName, requestRecord.userId))), RequestCounters(0, 0))

		requestRecord.requestStatus match {
			case OK => requestCounters.successfulCount += 1
			case KO => requestCounters.failedCount += 1
		}

		display(false)
	}

	override def onFlushDataWriter {
		if (!complete)
			display(true)
	}

	private def updateCurrentGroup(scenarioName: String, userId: Int, value: Option[Group] => Option[Group]) =
		groupStack.put((scenarioName, userId), value(groupStack.get((scenarioName, userId))))
}