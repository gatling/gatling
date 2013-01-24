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

import scala.collection.mutable.Map
import scala.math.{ ceil, floor, max }

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import com.excilys.ebi.gatling.core.util.PaddableStringBuilder
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

object ConsoleSummary {
	val iso8601Format = "yyyy-MM-dd HH:mm:ss"
	val dateTimeFormat = DateTimeFormat.forPattern(iso8601Format)

	val outputLength = 80
	val blockSeparator = "=" * outputLength

	def apply(elapsedTime: Long, usersCounters: Map[String, UserCounters], requestsCounters: Map[String, RequestCounters], time: DateTime = DateTime.now) = {

		def newBlock(buff: StringBuilder) { buff.append(blockSeparator).append(END_OF_LINE) }

		def appendTimeInfos(buff: StringBuilder, time: DateTime, elapsedTimeInSec: Long) {
			val now = ConsoleSummary.dateTimeFormat.print(time)
			buff.append(now)
				.appendLeftPaddedString(elapsedTimeInSec.toString, outputLength - iso8601Format.length - 9)
				.append("s elapsed")
				.append(END_OF_LINE)
		}

		def appendSubTitle(buff: StringBuilder, title: String) {
			buff.append("---- ").append(title).append(" ").appendTimes("-", max(outputLength - title.length - 6, 0)).append(END_OF_LINE)
		}

		def appendUsersProgressBar(buff: StringBuilder, usersStats: UserCounters) {
			val width = outputLength - 15

			val totalCount = usersStats.totalCount
			val runningCount = usersStats.runningCount
			val doneCount = usersStats.doneCount

			val donePercent = floor(100 * doneCount.toDouble / totalCount).toInt
			val done = floor(width * doneCount.toDouble / totalCount).toInt
			val running = ceil(width * runningCount.toDouble / totalCount).toInt
			val waiting = width - done - running

			buff.append("Users  : [").appendTimes("#", done).appendTimes("-", running).appendTimes(" ", waiting).append("]")
				.appendLeftPaddedString(donePercent.toString, 3).append("%")
				.append(END_OF_LINE)
		}

		def appendUserCounters(buff: StringBuilder, userCounters: UserCounters) {
			buff.append("          waiting:").appendRightPaddedString(userCounters.waitingCount.toString, 5)
				.append(" / running:").appendRightPaddedString(userCounters.runningCount.toString, 5)
				.append(" / done:").appendRightPaddedString(userCounters.doneCount.toString, 5)
				.append(END_OF_LINE)
		}

		def appendRequestCounters(buff: StringBuilder, actionName: String, requestCounters: RequestCounters) {
			buff.append("> ").appendRightPaddedString(actionName, outputLength - 22)
				.append(" OK=").appendRightPaddedString(requestCounters.successfulCount.toString, 6)
				.append(" KO=").appendRightPaddedString(requestCounters.failedCount.toString, 6)
				.append(END_OF_LINE)
		}

		val buff = new StringBuilder

		newBlock(buff)

		appendTimeInfos(buff, time, elapsedTime)

		//Users
		usersCounters.foreach {
			case (scenarioName, usersStats) => {
				appendSubTitle(buff, scenarioName)
				appendUsersProgressBar(buff, usersStats)
				appendUserCounters(buff, usersStats)
			}
		}

		//Requests
		appendSubTitle(buff, "Requests")
		requestsCounters.foreach {
			case (actionName, requestCounters) => {
				appendRequestCounters(buff, actionName, requestCounters)
			}
		}

		newBlock(buff)

		val complete = {
			val totalWaiting = usersCounters.values.map(_.waitingCount).sum
			val totalRunning = usersCounters.values.map(_.runningCount).sum
			(totalWaiting == 0) && (totalRunning == 0)
		}

		new ConsoleSummary(buff, complete)
	}
}

class ConsoleSummary(buff: StringBuilder, val complete: Boolean) {

	override def toString = buff.toString
}