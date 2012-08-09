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

import scala.collection.mutable.Map
import scala.math.{ ceil, floor, max }

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import com.excilys.ebi.gatling.core.util.PaddableStringBuilder.toPaddable
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

object ConsoleSummary {
	val iso8601Format = "yyyy-MM-dd HH:mm:ss"
	val dateTimeFormat = DateTimeFormat.forPattern(iso8601Format)

	def apply(elapsedTime: Long, usersCounters: Map[String, UserCounters], requestsCounters: Map[String, RequestCounters]) = {
		val summary = new ConsoleSummary(80)
		summary.newBlock

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
		summary.newBlock

		summary
	}
}

class ConsoleSummary(val outputLength: Int) {
	private val buff = new StringBuilder
	private val blockSeparator = "=" * outputLength

	def newBlock { buff.append(blockSeparator).append(END_OF_LINE) }

	def appendTimeInfos(elapsedTimeInSec: Long) {
		val now = ConsoleSummary.dateTimeFormat.print(new DateTime)
		buff.append(now)
			.appendLeftPaddedString(elapsedTimeInSec.toString, outputLength - ConsoleSummary.iso8601Format.length - 9)
			.append("s elapsed")
			.append(END_OF_LINE)
	}

	def appendSubTitle(title: String) {
		buff.append("---- ").append(title).append(" ").appendTimes("-", max(outputLength - title.length - 6, 0)).append(END_OF_LINE)
	}

	def appendUsersProgressBar(usersStats: UserCounters) {
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

	def appendUserCounters(userCounters: UserCounters) {
		buff.append("          waiting:").appendRightPaddedString(userCounters.waitingCount.toString, 5)
			.append(" / running:").appendRightPaddedString(userCounters.runningCount.toString, 5)
			.append(" / done:").appendRightPaddedString(userCounters.doneCount.toString, 5)
			.append(END_OF_LINE)
	}

	def appendRequestCounters(actionName: String, requestCounters: RequestCounters) {
		buff.append("> ").appendRightPaddedString(actionName, outputLength - 22)
			.append(" OK=").appendRightPaddedString(requestCounters.successfulCount.toString, 6)
			.append(" KO=").appendRightPaddedString(requestCounters.failedCount.toString, 6)
			.append(END_OF_LINE)
	}

	override def toString = buff.toString
}