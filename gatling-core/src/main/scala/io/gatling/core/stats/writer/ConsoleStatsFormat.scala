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

import io.gatling.commons.util.StringHelper.{ Eol, RichString }
import io.gatling.core.stats.{ ErrorStats, NoPlotMagicValue }
import io.gatling.shared.util.NumberHelper._

private[gatling] object ConsoleStatsFormat {

  val NumberLength: Int = 9 // max 9,999,999
  val PercentageLength: Int = 8 // max (99.99%)
  val ConsoleWidth: Int = 120
  val Header: String = "> "
  val HeaderLength: Int = Header.length
  val NewBlock: String = "=" * ConsoleWidth

  def formatNumber[T: Numeric](value: T): String = {
    val string = value match {
      case NoPlotMagicValue => "-"
      case _                => implicitly[Numeric[T]].toDouble(value).toPrintableString
    }
    string.leftPad(NumberLength)
  }

  def formatPercentage(value: Double): String =
    s"(${value.toPrintableString}%)".leftPad(PercentageLength)

  private val ErrorFirstLineLength = ConsoleWidth - HeaderLength - NumberLength - PercentageLength - 2
  private val ErrorSecondLineLength = ErrorFirstLineLength + HeaderLength

  def writeError(sb: jl.StringBuilder, errors: ErrorStats): jl.StringBuilder = {
    val message = errors.message
    val firstLineLen = math.min(message.length, ErrorFirstLineLength)

    sb.append(
      s"$Header${message.substring(0, firstLineLen).rightPad(ErrorFirstLineLength)} ${formatNumber(errors.count)} ${formatPercentage(errors.percentage)}"
    )

    if (message.length > ErrorFirstLineLength) {
      sb.append(Eol).append(message.substring(ErrorFirstLineLength).truncate(ErrorSecondLineLength))
    } else {
      sb
    }
  }

  def formatSubTitleWithStatuses(title: String): String = {
    val titleWrappedWithSpaces = s" $title "
    s"----${titleWrappedWithSpaces.rightPad(ConsoleWidth - 4 - 35, "-")}|---Total---|-----OK----|----KO----"
  }
}
