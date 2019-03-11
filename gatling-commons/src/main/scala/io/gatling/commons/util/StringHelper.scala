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

package io.gatling.commons.util

import java.lang.{ Long => JLong, StringBuilder => JStringBuilder }
import java.nio.charset.StandardCharsets._
import java.text.Normalizer

import io.gatling.commons.util.UnsafeHelper._

import com.dongxiguo.fastring.Fastring.Implicits._

object StringHelper {

  private val StringValueFieldOffset: Long = TheUnsafe.objectFieldOffset(classOf[String].getDeclaredField("value"))

  val Eol: String = System.lineSeparator
  val EolBytes: Array[Byte] = Eol.getBytes(US_ASCII)

  val Crlf: String = "\r\n"

  val EmptyFastring: Fastring = fast""

  def bytes2Hex(bytes: Array[Byte]): String = bytes.foldLeft(new JStringBuilder(bytes.length)) { (buff, b) =>
    val shifted = b & 0xff
    if (shifted < 0x10)
      buff.append("0")
    buff.append(JLong.toString(shifted.toLong, 16))
  }.toString

  implicit class RichString(val string: String) extends AnyVal {

    def clean: String = {
      val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
      normalized.toLowerCase.replaceAll("\\p{InCombiningDiacriticalMarks}+", "-").replaceAll("[^a-zA-Z0-9\\-]", "-")
    }

    def escapeJsIllegalChars: String = string.replace("\"", "\\\"").replace("\\", "\\\\")

    def trimToOption: Option[String] = string.trim match {
      case "" => None
      case s  => Some(s)
    }

    def truncate(maxLength: Int): String = if (string.length <= maxLength) string else string.substring(0, maxLength) + "..."

    def leftPad(length: Int, padder: String = " "): String = {
      val paddingLength = length - string.length
      if (paddingLength > 0)
        padder * paddingLength + string
      else
        string
    }

    def rightPad(length: Int, padder: String = " "): String = {
      val paddingLength = length - string.length
      if (paddingLength > 0)
        string + padder * paddingLength
      else
        string
    }

    def unsafeChars: Array[Char] =
      if (JavaRuntime.JavaMajorVersion == 8) {
        TheUnsafe.getObject(string, StringValueFieldOffset).asInstanceOf[Array[Char]]
      } else {
        string.toCharArray
      }
  }

  implicit class RichCharSequence(val source: CharSequence) extends AnyVal {

    def indexOf(target: Array[Char], fromIndex: Int): Int = {

      val sourceCount = source.length
      val targetCount = target.length

      if (fromIndex >= sourceCount) {
        if (targetCount == 0) sourceCount else -1

      } else if (targetCount == 0) {
        fromIndex

      } else {
        var i = fromIndex
        val first = target(0)
        val max = sourceCount - targetCount

        while (i <= max) {
          // Look for first character
          if (source.charAt(i) != first) {
            i += 1
            while (i <= max && source.charAt(i) != first) {
              i += 1
            }
          }

          // Found first character, now look at the rest of v2
          if (i <= max) {
            var j = i + 1
            val end = j + targetCount - 1
            var k = 1

            while (j < end && source.charAt(j) == target(k)) {
              j += 1
              k += 1
            }

            if (j == end) {
              // Found whole string
              return i
            }
          }

          i += 1
        }
        -1
      }
    }
  }
}
