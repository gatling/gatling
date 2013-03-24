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
package com.excilys.ebi.gatling.core.result.writer

import java.lang.System.currentTimeMillis

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.action.system.dispatcher
import com.excilys.ebi.gatling.core.result.RequestPath
import com.excilys.ebi.gatling.core.result.message.{ GroupRecord, KO, OK }
import com.excilys.ebi.gatling.core.result.message.{ RequestRecord, RunRecord, ScenarioRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.message.RecordEvent.{ END, START }

import grizzled.slf4j.Logging

case object Display

class UserCounters(val totalCount: Int) {

	private var _runningCount: Int = 0
	private var _doneCount: Int = 0

	def runningCount = _runningCount
	def doneCount = _doneCount

	def userStart { _runningCount += 1 }
	def userDone { _runningCount -= 1; _doneCount += 1 }
	def waitingCount = totalCount - _runningCount - _doneCount
}

class RequestCounters(var successfulCount: Int, var failedCount: Int)

class ConsoleDataWriter extends DataWriter with Logging {

	private var startUpTime = 0L

	private val usersCounters = mutable.Map.empty[String, UserCounters]
	private val groupStack = mutable.Map.empty[Int, List[String]]
	private val requestsCounters: mutable.Map[String, RequestCounters] = mutable.LinkedHashMap.empty

	private val displayPeriod = 5 seconds

	private var complete = false

	def display {
		val now = currentTimeMillis
		val timeSinceStartUpInSec = (now - startUpTime) / 1000

		val summary = ConsoleSummary(timeSinceStartUpInSec, usersCounters, requestsCounters)
		complete = summary.complete
		println(summary)
	}

	override def initialized: Receive = super.initialized.orElse {
		case Display => display
	}

	override def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {

		import system.dispatcher

		startUpTime = currentTimeMillis

		scenarios.foreach(scenario => usersCounters.put(scenario.name, new UserCounters(scenario.nbUsers)))

		system.scheduler.schedule(0 seconds, displayPeriod, self, Display)
	}

	override def onScenarioRecord(scenarioRecord: ScenarioRecord) {
		scenarioRecord.event match {
			case START =>
				usersCounters
					.get(scenarioRecord.scenarioName)
					.map(_.userStart)
					.getOrElse(error(s"Internal error, scenario '${scenarioRecord.scenarioName}' has not been correctly initialized"))

			case END =>
				usersCounters
					.get(scenarioRecord.scenarioName)
					.map(_.userDone)
					.getOrElse(error(s"Internal error, scenario '${scenarioRecord.scenarioName}' has not been correctly initialized"))
				groupStack.remove(scenarioRecord.userId)
		}
	}

	override def onGroupRecord(groupRecord: GroupRecord) {

		val userId = groupRecord.userId
		val userStack = groupStack.getOrElse(userId, Nil)

		val newUserStack = groupRecord.event match {
			case START => groupRecord.groupName :: userStack
			case END if (!userStack.isEmpty) => userStack.tail
			case _ =>
				error("Trying to stop a user that hasn't started?!")
				Nil
		}

		groupStack += userId -> newUserStack
	}

	override def onRequestRecord(requestRecord: RequestRecord) {

		val currentGroup = groupStack.getOrElse(requestRecord.userId, Nil)
		val requestPath = RequestPath.path(requestRecord.requestName :: currentGroup)
		val requestCounters = requestsCounters.getOrElseUpdate(requestPath, new RequestCounters(0, 0))

		requestRecord.requestStatus match {
			case OK => requestCounters.successfulCount += 1
			case KO => requestCounters.failedCount += 1
		}
	}

	override def onFlushDataWriter {
		if (!complete) display
	}
}