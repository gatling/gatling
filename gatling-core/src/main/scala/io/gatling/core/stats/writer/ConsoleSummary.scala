/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.{ lang => jl }
import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.mutable
import scala.math.{ ceil, floor }

import io.gatling.commons.shared.unstable.model.stats.ErrorStats
import io.gatling.commons.util.Collections._
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.GatlingConfiguration

object ConsoleSummary {

  private val Iso8601Format = "yyyy-MM-dd HH:mm:ss"
  private val Iso8601DateTimeFormat = new SimpleDateFormat(Iso8601Format)
  val OutputLength: Int = 80
  val NewBlock: String = "=" * OutputLength

  def writeSubTitle(sb: jl.StringBuilder, title: String): jl.StringBuilder =
    sb.append(("---- " + title + " ").rightPad(OutputLength, "-"))

  def apply(
      runDuration: Long,
      usersCounters: mutable.Map[String, UserCounters],
      globalRequestCounters: RequestCounters,
      requestsCounters: mutable.Map[String, RequestCounters],
      errorsCounters: mutable.Map[String, Int],
      configuration: GatlingConfiguration,
      time: Date
  ): ConsoleSummary = {

    def writeUsersCounters(sb: jl.StringBuilder, scenarioName: String, userCounters: UserCounters): jl.StringBuilder = {

      import userCounters._
      totalUserCount match {
        case Some(tot) if tot >= doneCount + activeCount =>
          val width = OutputLength - 6 // []3d%

          val donePercent = floor(100 * doneCount.toDouble / tot).toInt
          val done = floor(width * doneCount.toDouble / tot).toInt
          val active = ceil(width * activeCount.toDouble / tot).toInt
          val waiting = width - done - active
          writeSubTitle(sb, scenarioName)
            .append(Eol)
            .append('[')
            .append("#" * done)
            .append("-" * active)
            .append(" " * waiting)
            .append(']')
            .append(donePercent.toString.leftPad(3))
            .append('%')
            .append(Eol)
            .append("          waiting: ")
            .append(waitingCount.toString.rightPad(6))
            .append(" / active: ")
            .append(activeCount.toString.rightPad(6))
            .append(" / done: ")
            .append(doneCount.toString.rightPad(6))

        case _ =>
          // Don't display progression for closed workload model, nor when tot is broken, it doesn't make sense
          writeSubTitle(sb, scenarioName)
            .append(Eol)
            .append("          active: ")
            .append(activeCount.toString.rightPad(6))
            .append(" / done: ")
            .append(doneCount.toString.rightPad(6))
      }
    }

    def writeRequestsCounter(sb: jl.StringBuilder, actionName: String, requestCounters: RequestCounters): jl.StringBuilder = {

      import requestCounters._
      val maxActionNameLength = OutputLength - 24
      sb.append("> ")
        .append(actionName.truncate(maxActionNameLength - 3).rightPad(maxActionNameLength))
        .append(" (OK=")
        .append(successfulCount.toString.rightPad(6))
        .append(" KO=")
        .append(failedCount.toString.rightPad(6))
        .append(')')
    }

    def writeDetailedRequestsCounter(sb: jl.StringBuilder): jl.StringBuilder = {
      if (!configuration.data.console.light) {
        requestsCounters.foreach { case (actionName, requestCounters) => writeRequestsCounter(sb, actionName, requestCounters).append(Eol) }
        if (requestsCounters.nonEmpty) {
          sb.setLength(sb.length - Eol.length)
        }
      }
      sb
    }

    def writeErrors(sb: jl.StringBuilder): jl.StringBuilder = {
      if (errorsCounters.nonEmpty) {
        val errorsTotal = errorsCounters.values.sum

        writeSubTitle(sb, "Errors").append(Eol)

        errorsCounters.toSeq.sortBy(-_._2).foreach { case (message, count) =>
          ConsoleErrorsWriter.writeError(sb, new ErrorStats(message, count, errorsTotal)).append(Eol)
        }
      }
      sb
    }

    val sb = new jl.StringBuilder()
      .append(Eol)
      .append(NewBlock)
      .append(Eol)
      .append(ConsoleSummary.Iso8601DateTimeFormat.format(time))
      .append(' ')
      .append((runDuration.toString + "s elapsed").leftPad(OutputLength - Iso8601Format.length - 9))
      .append(Eol)

    writeSubTitle(sb, "Requests").append(Eol)
    writeRequestsCounter(sb, "Global", globalRequestCounters).append(Eol)
    writeDetailedRequestsCounter(sb).append(Eol)
    writeErrors(sb).append(Eol)

    usersCounters.foreach { case (scenarioName, usersStats) =>
      writeUsersCounters(sb, scenarioName, usersStats).append(Eol)
    }

    sb.append(NewBlock).append(Eol)

    val text = sb.toString

    val complete = {
      val totalWaiting = usersCounters.values.sumBy(_.waitingCount)
      val totalRunning = usersCounters.values.sumBy(_.activeCount)
      (totalWaiting == 0) && (totalRunning == 0)
    }

    new ConsoleSummary(text, complete)
  }
}

final class ConsoleSummary(val text: String, val complete: Boolean)
