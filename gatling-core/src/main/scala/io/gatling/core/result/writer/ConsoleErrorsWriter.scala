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

import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.core.util.StringHelper._

/**
 * Object for writing errors statistics to the console.
 */
object ConsoleErrorsWriter {
  val errorCountLen = 14
  val errorMsgLen = ConsoleSummary.outputLength - errorCountLen

  def writeError(msg: String, count: Int, totalCount: Int): Fastring = {
    val percent = if (count == totalCount) "100.0" else f"${count.toDouble * 100 / totalCount}%3.2f"

    val currLen = errorMsgLen - 4;
    val firstLineLen = currLen.min(msg.length)
    val firstLine = fast"> ${msg.substring(0, firstLineLen).rightPad(currLen)} ${count.filled(6)} (${percent.leftPad(5)}%)"

    if (currLen < msg.length) {
      val secondLine = msg.substring(currLen)
      fast"$firstLine$eol${secondLine.truncate(errorMsgLen - 4)}"
    } else {
      firstLine
    }
  }
}
