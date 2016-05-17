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
package io.gatling.commons.util

import java.lang.{ Long => JLong, StringBuilder => JStringBuilder }
import java.nio.charset.StandardCharsets._
import java.security.MessageDigest
import java.text.Normalizer

import scala.util.Try

import io.gatling.commons.util.UnsafeHelper._

import com.dongxiguo.fastring.Fastring.Implicits._

/**
 * This object groups all utilities for strings
 */
object StringHelper {

  private val StringValueFieldOffset: Long = TheUnsafe.objectFieldOffset(classOf[String].getDeclaredField("value"))
  private val StringOffsetFieldOffset: Option[Long] = Try(TheUnsafe.objectFieldOffset(classOf[String].getDeclaredField("offset"))).toOption

  val Eol = System.getProperty("line.separator")
  val EolBytes = Eol.getBytes(US_ASCII)

  val Crlf = "\r\n"

  val EmptyFastring = fast""

  val EmptyCharSequence = ArrayCharSequence(Array.empty[Char])

  def bytes2Hex(bytes: Array[Byte]): String = bytes.foldLeft(new JStringBuilder(bytes.length)) { (buff, b) =>
    val shifted = b & 0xff
    if (shifted < 0x10)
      buff.append("0")
    buff.append(JLong.toString(shifted.toLong, 16))
  }.toString

  private val StringBuilderPool = new ThreadLocal[StringBuilder] {
    override def initialValue() = new StringBuilder(512)
  }

  /**
   * BEWARE: MUSN'T APPEND TO ITSELF!
   * @return a pooled StringBuilder
   */
  def stringBuilder(): StringBuilder = {
    val sb = StringBuilderPool.get
    sb.setLength(0)
    sb
  }

  implicit class RichString(val string: String) extends AnyVal {

    def clean = {
      val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
      normalized.toLowerCase.replaceAll("\\p{InCombiningDiacriticalMarks}+", "-").replaceAll("[^a-zA-Z0-9\\-]", "-")
    }

    def escapeJsIllegalChars = string.replace("\"", "\\\"").replace("\\", "\\\\")

    def trimToOption = string.trim match {
      case "" => None
      case s  => Some(s)
    }

    def truncate(maxLength: Int) = if (string.length <= maxLength) string else string.substring(0, maxLength) + "..."

    def leftPad(length: Int, padder: String = " ") = {
      val paddingLength = length - string.length
      if (paddingLength > 0)
        padder * paddingLength + string
      else
        string
    }

    def rightPad(length: Int, padder: String = " ") = {
      val paddingLength = length - string.length
      if (paddingLength > 0)
        string + padder * paddingLength
      else
        string
    }

    def unsafeChars: Array[Char] = TheUnsafe.getObject(string, StringValueFieldOffset).asInstanceOf[Array[Char]]

    def isConstantTimeEqual(other: String): Boolean =
      MessageDigest.isEqual(string.getBytes(UTF_8), other.getBytes(UTF_8))
  }

  implicit class RichCharSequence(val source: CharSequence) extends AnyVal {

    def indexOf(s: String, fromIndex: Int): Int = {

      val sourceCount = source.length
      val target = s.unsafeChars
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

    def startWith(chars: Array[Char]): Boolean = {

      if (chars.length > source.length)
        false

      else {
        var i = 0
        while (i < source.length && i < chars.length) {
          if (source.charAt(i) != chars(i))
            return false
          i += 1
        }

        i == chars.length
      }
    }

    def contains(f: Char => Boolean): Boolean = {
      for (i <- 0 until source.length) {
        if (f(source.charAt(i)))
          return true
      }

      false
    }
  }
}
