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

package io.gatling.commons.util

import java.{ lang => jl }
import java.nio.charset.StandardCharsets._
import java.text.Normalizer
import java.util.Locale

import io.gatling.commons.util.Spire.cfor
import io.gatling.netty.util.StringBuilderPool

object StringHelper {

  val Eol: String = System.lineSeparator
  val EolBytes: Array[Byte] = Eol.getBytes(US_ASCII)

  val Crlf: String = "\r\n"

  object RichString {
    private val SbPool = new StringBuilderPool
  }

  implicit class RichString(val string: String) extends AnyVal {

    def clean: String = {
      val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
      normalized.toLowerCase(Locale.ROOT).replaceAll("\\p{InCombiningDiacriticalMarks}+", "-").replaceAll("[^a-zA-Z0-9\\-]", "-")
    }

    def trimToOption: Option[String] = string.trim match {
      case "" => None
      case s  => Some(s)
    }

    def truncate(maxLength: Int): String = if (string.length <= maxLength) string else string.substring(0, maxLength) + "..."

    def leftPad(length: Int): String = leftPad(length, " ")
    def leftPad(length: Int, padder: String): String = {
      val paddingLength = length - string.length
      if (paddingLength > 0)
        padder * paddingLength + string
      else
        string
    }

    def rightPad(length: Int): String = rightPad(length, " ")
    def rightPad(length: Int, padder: String): String = {
      val paddingLength = length - string.length
      if (paddingLength > 0)
        string + padder * paddingLength
      else
        string
    }

    def replaceIf(replaced: Char => Boolean, replacement: Char): String =
      if (string.isEmpty) {
        string
      } else {
        var matchFound = false
        var sb: jl.StringBuilder = null

        cfor(0)(_ < string.length, _ + 1) { i =>
          val c = string.charAt(i)
          if (replaced(c)) {
            if (!matchFound) {
              // first match
              sb = RichString.SbPool.get()
              sb.append(string, 0, i)
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
          string
        }
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
        var exit = false
        while (i <= max && !exit) {
          // look for first character
          if (source.charAt(i) != first) {
            i += 1
            while (i <= max && source.charAt(i) != first) {
              i += 1
            }
          }

          // found first character, now look at the rest of target
          if (i <= max) {
            var j = i + 1
            val end = j + targetCount - 1
            var k = 1

            while (j < end && source.charAt(j) == target(k)) {
              j += 1
              k += 1
            }

            if (j == end) {
              // found whole string
              exit = true
            }
          }

          if (!exit) {
            i += 1
          }
        }
        if (exit) i else -1
      }
    }
  }
}
