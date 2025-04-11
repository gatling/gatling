/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

import scala.collection.mutable
import scala.math.{ ceil, floor }

import io.gatling.commons.util.Collections._
import io.gatling.commons.util.StringHelper._
import io.gatling.core.stats.ErrorStats
import io.gatling.core.stats.writer.ConsoleStatsFormat._
import io.gatling.shared.util.NumberHelper._

private[gatling] object ConsoleSummary {

  def formatSubTitle(title: String): String =
    s"---- $title ".rightPad(ConsoleWidth, "-")

  def apply(
      runDuration: Long,
      usersCounters: mutable.Map[String, UserCounters],
      globalRequestCounters: RequestCounters,
      requestsCounters: mutable.Map[String, RequestCounters],
      errorsCounters: mutable.Map[String, Int],
      lightOutput: Boolean,
      time: TemporalAccessor,
      dateTimeFormatter: DateTimeFormatter
  ): ConsoleSummary = {
    def writeUsersCounters(sb: jl.StringBuilder, scenarioName: String, userCounters: UserCounters): jl.StringBuilder = {
      import userCounters._
      totalUserCount match {
        case Some(tot) if tot >= doneCount + activeCount =>
          val width = ConsoleWidth - 8 // []99.99%

          val donePercent = 100 * doneCount.toDouble / tot
          val done = floor(width * doneCount.toDouble / tot).toInt
          val active = ceil(width * activeCount.toDouble / tot).toInt
          val waiting = width - done - active
          sb.append(
            s"""${formatSubTitle(scenarioName)}
               |[${"#" * done}${"|" * active}${" " * waiting}]${s"${donePercent.toPrintableString}%".leftPad(6)}
               |          waiting: ${formatNumber(waitingCount)} / active: ${formatNumber(activeCount)}  / done: ${formatNumber(doneCount)}""".stripMargin
          )

        case _ =>
          // Don't display progression for closed workload model, nor when tot is broken, it doesn't make sense
          sb.append(s"""${formatSubTitle(scenarioName)}
                       |          active: ${formatNumber(activeCount)}  / done: ${formatNumber(doneCount)}""".stripMargin)
      }
    }

    def writeRequestsCounter(sb: jl.StringBuilder, actionName: String, requestCounters: RequestCounters): jl.StringBuilder = {
      import requestCounters._
      val maxActionNameLength = ConsoleWidth - HeaderLength - 3 * (NumberLength + 3)
      sb.append(
        s"$Header${actionName.truncate(maxActionNameLength).rightPad(maxActionNameLength)} | ${formatNumber(successfulCount + failedCount)} | ${formatNumber(successfulCount)} | ${formatNumber(failedCount)}"
      )
    }

    def writeDetailedRequestsCounter(sb: jl.StringBuilder): jl.StringBuilder = {
      if (!lightOutput) {
        requestsCounters.foreachEntry((actionName, requestCounters) => writeRequestsCounter(sb, actionName, requestCounters).append(Eol))
        if (requestsCounters.nonEmpty) {
          sb.setLength(sb.length - Eol.length)
        }
      }
      sb
    }

    def writeErrors(sb: jl.StringBuilder): jl.StringBuilder = {
      if (errorsCounters.nonEmpty) {
        val errorsTotal = errorsCounters.values.sum

        sb.append(formatSubTitle("Errors")).append(Eol)

        errorsCounters.toSeq.sortBy(-_._2).foreach { case (message, count) =>
          writeError(sb, new ErrorStats(message, count, errorsTotal)).append(Eol)
        }
      }
      sb
    }

    val formattedTime = dateTimeFormatter.format(time)
    val sb = new jl.StringBuilder()
      .append(s"""
                 |$NewBlock
                 |$formattedTime ${(runDuration.toString + "s elapsed").leftPad(ConsoleWidth - formattedTime.length - 9)}
                 |${formatSubTitleWithStatuses("Requests")}
                 |""".stripMargin)

    writeRequestsCounter(sb, "Global", globalRequestCounters).append(Eol)
    writeDetailedRequestsCounter(sb).append(Eol)
    writeErrors(sb).append(Eol)

    usersCounters.foreachEntry { (scenarioName, usersStats) =>
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

private[gatling] final class ConsoleSummary(val text: String, val complete: Boolean)
