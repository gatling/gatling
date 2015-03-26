/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.mutable
import scala.math.{ ceil, floor }

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.StringHelper._
import io.gatling.core.result.ErrorStats

object ConsoleSummary {

  val Iso8601Format = "yyyy-MM-dd HH:mm:ss"
  val Iso8601DateTimeFormat = new SimpleDateFormat(Iso8601Format)
  val OutputLength = 80
  val NewBlock = "=" * OutputLength

  def writeSubTitle(title: String): Fastring = fast"${("---- " + title + " ").rightPad(OutputLength, "-")}"

  def apply(runDuration: Long,
            usersCounters: mutable.Map[String, UserCounters],
            globalRequestCounters: RequestCounters,
            requestsCounters: mutable.Map[String, RequestCounters],
            errorsCounters: mutable.Map[String, Int],
            configuration: GatlingConfiguration,
            time: Date = new Date) = {

      def writeUsersCounters(scenarioName: String, userCounters: UserCounters): Fastring = {

        import userCounters._

        val width = OutputLength - 6 // []3d%

        val donePercent = floor(100 * doneCount.toDouble / totalCount).toInt
        val done = floor(width * doneCount.toDouble / totalCount).toInt
        val active = ceil(width * activeCount.toDouble / totalCount).toInt
        val waiting = width - done - active

        fast"""${writeSubTitle(scenarioName)}
[${"#" * done}${"-" * active}${" " * waiting}]${donePercent.toString.leftPad(3)}%
          waiting: ${waitingCount.toString.rightPad(6)} / active: ${activeCount.toString.rightPad(6)} / done:${doneCount.toString.rightPad(6)}"""
      }

      def writeRequestsCounter(actionName: String, requestCounters: RequestCounters): Fastring = {

        import requestCounters._

        fast"> ${actionName.rightPad(OutputLength - 24)} (OK=${successfulCount.toString.rightPad(6)} KO=${failedCount.toString.rightPad(6)})"
      }

      def writeDetailedRequestsCounter: Fastring =
        if (configuration.data.console.light)
          EmptyFastring
        else
          requestsCounters.map { case (actionName, requestCounters) => writeRequestsCounter(actionName, requestCounters) }.mkFastring(Eol)

      def writeErrors: Fastring =
        if (errorsCounters.nonEmpty)
          fast"""${writeSubTitle("Errors")}
${errorsCounters.toVector.sortBy(-_._2).map(err => ConsoleErrorsWriter.writeError(ErrorStats(err._1, err._2, globalRequestCounters.failedCount))).mkFastring(Eol)}
"""
        else
          EmptyFastring

    val text = fast"""
$NewBlock
${ConsoleSummary.Iso8601DateTimeFormat.format(time)} ${(runDuration + "s elapsed").leftPad(OutputLength - Iso8601Format.length - 9)}
${usersCounters.map { case (scenarioName, usersStats) => writeUsersCounters(scenarioName, usersStats) }.mkFastring(Eol)}
${writeSubTitle("Requests")}
${writeRequestsCounter("Global", globalRequestCounters)}
$writeDetailedRequestsCounter
$writeErrors$NewBlock
""".toString

    val complete = {
      val totalWaiting = usersCounters.values.map(_.waitingCount).sum
      val totalRunning = usersCounters.values.map(_.activeCount).sum
      (totalWaiting == 0) && (totalRunning == 0)
    }

    new ConsoleSummary(text, complete)
  }
}

case class ConsoleSummary(text: String, complete: Boolean)
