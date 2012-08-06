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

import scala.collection.mutable.{ LinkedHashMap, Map, HashMap }
import scala.math.{ max, floor, ceil }

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.message.{ RequestRecord, InitializeDataWriter, FlushDataWriter }
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

import grizzled.slf4j.Logging

class UserCounters(val totalCount: Int, private[this] var _runningCount: Int = 0, private[this] var _doneCount: Int = 0) {
	def runningCount = _runningCount
	def doneCount = _doneCount

	def userStart = _runningCount += 1
	def userDone: Unit = { _runningCount -= 1; _doneCount += 1 }
	def waitingCount = totalCount - _runningCount - _doneCount
}

case class RequestCounters(var successfulCount: Int, var failedCount: Int)

object ConsoleSummary {
	val iso8601Format = "yyyy-mm-dd HH:MM:SS"
	val dateTimeFormat = DateTimeFormat.forPattern(iso8601Format)

	def apply(elapsedTime: Long, usersCounters: Map[String, UserCounters], requestsCounters: Map[String, RequestCounters]) = {
		val summary = new ConsoleSummary(80)
		summary.newBlock()

		summary.appendTimeInfos(elapsedTime)

		//Users
		usersCounters.foreach {
			case (scenarioName, usersStats) => {
				summary.appendSubTitle(scenarioName)
				summary.appendUsersProgressBar(usersStats)
				summary.appendUserCounters(usersStats)
			}
		}

		//Requests
		summary.appendSubTitle("Requests")
		requestsCounters.foreach {
			case (actionName, requestCounters) => {
				summary.appendRequestCounters(actionName, requestCounters)
			}
		}
		summary.newBlock()

		summary
	}
}

class ConsoleSummary(val outputLength: Int) {
	private val buff = new StringBuilder

	private val blockSeparator = "=" * outputLength
	private val requestPattern = "> %-" + (outputLength - 23) + "s  OK=%-6d KO=%-6d"
	private val usersPattern = "          waiting:%-5d / running:%-5d / done:%-5d"
	private val timePattern = "%s %" + (outputLength - ConsoleSummary.iso8601Format.length - 10) + "ds elapsed"

	def newBlock(): Unit = buff.append(blockSeparator).append(END_OF_LINE)

	def appendTimeInfos(elapsedTimeInSec: Long): Unit = {
		val now = ConsoleSummary.dateTimeFormat.print(new DateTime)
		buff.append(timePattern.format(now, elapsedTimeInSec)).append(END_OF_LINE)
	}

	def appendSubTitle(title: String) =
		buff.append("---- ").append(title).append(" ").append("-" * max(outputLength - title.length - 6, 0)).append(END_OF_LINE)

	def appendUsersProgressBar(usersStats: UserCounters) = {
		val width = outputLength - 15

		val totalCount = usersStats.totalCount
		val runningCount = usersStats.runningCount
		val doneCount = usersStats.doneCount

		val donePercent = floor(100 * doneCount.toDouble / totalCount).toInt
		val done = floor(width * doneCount.toDouble / totalCount).toInt
		val running = ceil(width * runningCount.toDouble / totalCount).toInt
		val waiting = width - done - running

		buff.append("Users  : [").append("#" * done).append("-" * running).append(" " * waiting).append("]")
			.append("%3d" format (donePercent)).append("%")
			.append(END_OF_LINE)
	}

	def appendUserCounters(userCounters: UserCounters) =
		buff.append(usersPattern format (userCounters.waitingCount.intValue(), userCounters.runningCount.intValue(), userCounters.doneCount.intValue()))
			.append(END_OF_LINE)

	def appendRequestCounters(actionName: String, requestCounters: RequestCounters) =
		buff.append(requestPattern format (actionName, requestCounters.successfulCount.intValue(), requestCounters.failedCount.intValue()))
			.append(END_OF_LINE)

	override def toString = buff.toString()
}

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

			usersCounters.clear()
			scenarios.foreach(scenario => usersCounters.put(scenario.name, new UserCounters(scenario.nbUsers)))
			requestsCounters.clear()

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