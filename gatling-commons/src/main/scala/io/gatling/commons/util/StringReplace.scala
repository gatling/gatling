/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.commons.util

import java.lang.{ StringBuilder => JStringBuilder }

import io.gatling.spire.syntax.cfor._

object StringReplace {

  private val SbPool = new StringBuilderPool

  def replace(text: String, replaced: String, replacement: String): String =
    if (text.isEmpty || replaced.isEmpty) {
      text
    } else {
      var end = text.indexOf(replaced)
      if (end == -1) {
        text
      } else {
        var start = 0
        val replacedLength = replaced.length
        val buf = SbPool.get()
        while (end != -1) {
          buf.append(text, start, end).append(replacement)
          start = end + replacedLength
          end = text.indexOf(replaced, start)
        }
        buf.append(text, start, text.length).toString
      }
    }

  def replace(text: String, replaced: Char => Boolean, replacement: Char): String =
    if (text.isEmpty) {
      text
    } else {
      var matchFound = false
      var sb: JStringBuilder = null

      cfor(0)(_ < text.length, _ + 1) { i =>
        val c = text.charAt(i)
        if (replaced(c)) {
          if (!matchFound) {
            // first match
            sb = StringBuilderPool.Global.get()
            sb.append(text, 0, i)
            matchFound = true
          }
          sb.append(replacement)
        } else if (matchFound) {
          sb.append(c)
        }
      }

      if (matchFound) {
        sb.toString
      } else {
        text
      }
    }
}
