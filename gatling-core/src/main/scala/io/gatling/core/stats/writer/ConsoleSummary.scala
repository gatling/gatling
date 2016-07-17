/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.stats.writer

import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.mutable
import scala.math.{ ceil, floor }

import io.gatling.commons.stats.ErrorStats
import io.gatling.commons.util.Collections._
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.GatlingConfiguration

import com.dongxiguo.fastring.Fastring.Implicits._

object ConsoleSummary {

  val Iso8601Format = "yyyy-MM-dd HH:mm:ss"
  val Iso8601DateTimeFormat = new SimpleDateFormat(Iso8601Format)
  val OutputLength = 80
  val NewBlock = "=" * OutputLength

  def writeSubTitle(title: String): Fastring = fast"${("---- " + title + " ").rightPad(OutputLength, "-")}"

  def apply(
    runDuration:           Long,
    usersCounters:         mutable.Map[String, UserCounters],
    globalRequestCounters: RequestCounters,
    requestsCounters:      mutable.Map[String, RequestCounters],
    errorsCounters:        mutable.Map[String, Int],
    configuration:         GatlingConfiguration,
    time:                  Date                                 = new Date
  ) = {

      def writeUsersCounters(scenarioName: String, userCounters: UserCounters): Fastring = {

        import userCounters._

        val width = OutputLength - 6 // []3d%

        val donePercent = floor(100 * doneCount.toDouble / userCount).toInt
        val done = floor(width * doneCount.toDouble / userCount).toInt
        val active = ceil(width * activeCount.toDouble / userCount).toInt
        val waiting = width - done - active

        fast"""${writeSubTitle(scenarioName)}
[${"#" * done}${"-" * active}${" " * waiting}]${donePercent.toString.leftPad(3)}%
          waiting: ${waitingCount.toString.rightPad(6)} / active: ${activeCount.toString.rightPad(6)} / done:${doneCount.toString.rightPad(6)}"""
      }

      def writeRequestsCounter(actionName: String, requestCounters: RequestCounters): Fastring = {

        import requestCounters._
        val maxActionNameLength = OutputLength - 24

        fast"> ${actionName.truncate(maxActionNameLength - 3).rightPad(maxActionNameLength)} (OK=${successfulCount.toString.rightPad(6)} KO=${failedCount.toString.rightPad(6)})"
      }

      def writeDetailedRequestsCounter: Fastring =
        if (configuration.data.console.light)
          EmptyFastring
        else
          requestsCounters.map { case (actionName, requestCounters) => writeRequestsCounter(actionName, requestCounters) }.mkFastring(Eol)

      def writeErrors: Fastring =
        if (errorsCounters.nonEmpty)
          fast"""${writeSubTitle("Errors")}
${errorsCounters.toVector.sortBy(-_._2).map { case (message, count) => ConsoleErrorsWriter.writeError(ErrorStats(message, count, globalRequestCounters.failedCount)) }.mkFastring(Eol)}
"""
        else
          EmptyFastring

    val text = fast"""
$NewBlock
${ConsoleSummary.Iso8601DateTimeFormat.format(time)} ${(runDuration + "s elapsed").leftPad(OutputLength - Iso8601Format.length - 9)}
${writeSubTitle("Requests")}
${writeRequestsCounter("Global", globalRequestCounters)}
$writeDetailedRequestsCounter
$writeErrors
${usersCounters.map { case (scenarioName, usersStats) => writeUsersCounters(scenarioName, usersStats) }.mkFastring(Eol)}
$NewBlock
""".toString

    val complete = {
      val totalWaiting = usersCounters.values.sumBy(_.waitingCount)
      val totalRunning = usersCounters.values.sumBy(_.activeCount)
      (totalWaiting == 0) && (totalRunning == 0)
    }

    new ConsoleSummary(text, complete)
  }
}

case class ConsoleSummary(text: String, complete: Boolean)
