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
import scala.collection.mutable.LinkedList

/**
 * Object for writing errors statistics to the console.
 *
 * @author Ivan Mushketyk
 */
object ConsoleErrorsWriter {
  val errorCountLen = 14
  val errorMsgLen = ConsoleSummary.outputLength - errorCountLen

  def writeHeader() = {
    fast"${"msg".rightPad(errorMsgLen)}count"
  }

  def writeError(msg: String, count: Int, percent: Double): Fastring = {
    val percentStr = f"$percent%3.2f"

    var currLen = errorMsgLen - 3;
    val firstLineLen = Math.min(msg.length, currLen)
    var lines = LinkedList(fast"> ${msg.substring(0, firstLineLen).rightPad(currLen)} ${count.toString.rightPad(5)} ${percentStr.leftPad(6)} %")

    if (currLen < msg.length) {
      val restLine = msg.substring(currLen)
      lines = lines :+ fast"${restLine.truncate(errorMsgLen - 4)}"
    }

    lines.mkFastring(eol)
  }
}
