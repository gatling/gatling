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

import io.gatling.commons.stats.ErrorStats
import io.gatling.commons.util.StringHelper._

import com.dongxiguo.fastring.Fastring.Implicits._

/**
 * Object for writing errors statistics to the console.
 */
object ConsoleErrorsWriter {
  val ErrorCountLen = 14
  val ErrorMsgLen = ConsoleSummary.OutputLength - ErrorCountLen
  val TextLen = ErrorMsgLen - 4

  def formatPercent(percent: Double): String = f"$percent%3.2f"

  val OneHundredPercent: String = formatPercent(100).dropRight(1)
  def writeError(errors: ErrorStats): Fastring = {
    val ErrorStats(msg, count, _) = errors

    val percent = if (errors.count == errors.totalCount) OneHundredPercent else formatPercent(errors.percentage)
    val firstLineLen = TextLen.min(msg.length)
    val firstLine = fast"> ${msg.substring(0, firstLineLen).rightPad(TextLen)} ${count.filled(6)} (${percent.leftPad(5)}%)"

    if (msg.length > TextLen) {
      val secondLine = msg.substring(TextLen)
      fast"$firstLine$Eol${secondLine.truncate(TextLen)}"
    } else {
      firstLine
    }
  }
}
