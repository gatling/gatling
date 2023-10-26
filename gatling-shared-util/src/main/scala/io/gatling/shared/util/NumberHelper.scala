/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.shared.util

import java.text.{ DecimalFormat, DecimalFormatSymbols }
import java.util.Locale.ENGLISH

// can't be moved to gatling-commons because of AssertionModel
object NumberHelper {
  private val Formatter = new DecimalFormat("###.###", DecimalFormatSymbols.getInstance(ENGLISH))

  implicit class RichDouble(val double: Double) extends AnyVal {
    private def suffix(i: Int) = i    % 10 match {
      case _ if (11 to 13) contains i % 100 => "th"
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"
    }

    def toRank: String =
      if (double == Math.floor(double)) {
        toPrintableString + suffix(double.toInt)
      } else {
        toPrintableString + suffix((double * 100).toInt % 100)
      }

    def toPrintableString: String = Formatter.format(double)
  }
}
