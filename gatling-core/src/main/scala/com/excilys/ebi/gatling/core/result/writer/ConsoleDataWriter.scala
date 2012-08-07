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

import scala.collection.mutable.{ HashMap, LinkedHashMap }

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.result.message.{ FlushDataWriter, InitializeDataWriter, RequestRecord }
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
	private val requestsCounters = new LinkedHashMap[String, RequestCounters]

	private val displayPeriod = 5 * 1000

	def uninitialized: Receive = {
		case InitializeDataWriter(_, scenarios, _, _) =>

			startUpTime = currentTimeMillis
			lastDisplayTime = currentTimeMillis

			context.become(initialized)

			usersCounters.clear
			scenarios.foreach(scenario => usersCounters.put(scenario.name, new UserCounters(scenario.nbUsers)))
			requestsCounters.clear

		case unknown: AnyRef => error("Unsupported message type in uninilialized state" + unknown.getClass)
		case unknown: Any => error("Unsupported message type in uninilialized state " + unknown)
	}

	def initialized: Receive = {
		case RequestRecord(scenarioName, userId, actionName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage, extraInfo) =>

			actionName match {
				case START_OF_SCENARIO => usersCounters.get(scenarioName) match {
					case Some(userStatus) => userStatus.userStart
					case None => error("Internal error, scenario '%s' has not been correctly initialized" format scenarioName)
				}

				case END_OF_SCENARIO => usersCounters.get(scenarioName) match {
					case Some(userStatus) => userStatus.userDone
					case None => error("Internal error, scenario '%s' has not been correctly initialized" format scenarioName)
				}

				case _ => {
					val requestCounters = requestsCounters.getOrElseUpdate(actionName, RequestCounters(0, 0))

					resultStatus match {
						case OK => requestCounters.successfulCount += 1
						case KO => requestCounters.failedCount += 1
					}
				}
			}

			val now = currentTimeMillis
			if (now - lastDisplayTime > displayPeriod) {
				lastDisplayTime = now
				val timeSinceStartUpInSec = (now - startUpTime) / 1000

				println(ConsoleSummary(timeSinceStartUpInSec, usersCounters, requestsCounters))
			}

		case FlushDataWriter => context.unbecome() // return to uninitialized state

		case unknown: AnyRef => error("Unsupported message type in inilialized state " + unknown.getClass)
		case unknown: Any => error("Unsupported message type in inilialized state " + unknown)
	}

	def receive = uninitialized
}