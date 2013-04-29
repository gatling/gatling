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

import scala.collection.mutable.Map
import scala.math.{ ceil, floor }

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.util.StringHelper.{ RichString, eol }

object ConsoleSummary {

	val iso8601Format = "yyyy-MM-dd HH:mm:ss"
	val dateTimeFormat = DateTimeFormat.forPattern(iso8601Format)
	val outputLength = 80
	val newBlock = "=" * outputLength

	def writeSubTitle(title: String) = ("---- " + title + " ").rightPad(outputLength, "-")

	def apply(runDuration: Long, usersCounters: Map[String, UserCounters], requestsCounters: Map[String, RequestCounters], time: DateTime = DateTime.now) = {

		def writeUsersCounters(scenarioName: String, userCounters: UserCounters): Fastring = {

			import userCounters._

			val width = outputLength - 6 // []3d%

			val donePercent = floor(100 * doneCount.toDouble / totalCount).toInt
			val done = floor(width * doneCount.toDouble / totalCount).toInt
			val running = ceil(width * runningCount.toDouble / totalCount).toInt
			val waiting = width - done - running

			fast"""${writeSubTitle(scenarioName)}
[${"#" * done}${"-" * running}${" " * waiting}]${donePercent.toString.leftPad(3)}%
          waiting: ${waitingCount.toString.rightPad(5)} / running: ${runningCount.toString.rightPad(5)} / done:${doneCount.toString.rightPad(5)}"""
		}

		def writeRequestsCounter(actionName: String, requestCounters: RequestCounters): Fastring = {

			import requestCounters._

			fast"> ${actionName.rightPad(outputLength - 22)} OK=${successfulCount.toString.rightPad(6)} KO=$failedCount"
		}

		val text = fast"""
$newBlock
${ConsoleSummary.dateTimeFormat.print(time)} ${(runDuration + "s elapsed").leftPad(outputLength - iso8601Format.length - 9)}
${usersCounters.map { case (scenarioName, usersStats) => writeUsersCounters(scenarioName, usersStats) }.mkFastring(eol)}
${writeSubTitle("Requests")}
${requestsCounters.map { case (actionName, requestCounters) => writeRequestsCounter(actionName, requestCounters) }.mkFastring(eol)}
$newBlock
""".toString

		val complete = {
			val totalWaiting = usersCounters.values.map(_.waitingCount).sum
			val totalRunning = usersCounters.values.map(_.runningCount).sum
			(totalWaiting == 0) && (totalRunning == 0)
		}

		new ConsoleSummary(text, complete)
	}
}

case class ConsoleSummary(text: String, complete: Boolean)