/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import java.lang.{ StringBuilder => JStringBuilder }

import io.gatling.commons.stats.ErrorStats
import io.gatling.commons.util.StringHelper._

/**
 * Object for writing errors statistics to the console.
 */
object ConsoleErrorsWriter {
  private val ErrorCountLen = 14
  private val ErrorMsgLen = ConsoleSummary.OutputLength - ErrorCountLen
  private val TextLen = ErrorMsgLen - 4

  def formatPercent(percent: Double): String = f"$percent%3.2f"

  val OneHundredPercent: String = formatPercent(100).dropRight(1)

  def writeError(sb: JStringBuilder, errors: ErrorStats): JStringBuilder = {
    val ErrorStats(msg, count, _) = errors
    val percent = if (errors.count == errors.totalCount) OneHundredPercent else formatPercent(errors.percentage)
    val firstLineLen = TextLen.min(msg.length)

    sb.append("> ").append(msg.substring(0, firstLineLen).rightPad(TextLen)).append(' ').append(count.toString.leftPad(6)).append(" (").append(percent.leftPad(5)).append("%)")

    if (msg.length > TextLen) {
      sb.append(Eol).append(msg.substring(TextLen).truncate(TextLen))
    } else {
      sb
    }
  }
}
