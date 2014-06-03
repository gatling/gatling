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
package io.gatling.core.util

import java.lang.{ Long => JLong, StringBuilder => JStringBuilder }
import java.text.Normalizer
import scala.util.Try

import io.gatling.core.util.UnsafeHelper._
import com.dongxiguo.fastring.Fastring.Implicits._

/**
 * This object groups all utilities for strings
 */
object StringHelper {

  sealed trait StringImplementation
  case object DirectCharsBasedStringImplementation extends StringImplementation
  case object OffsetBasedStringImplementation extends StringImplementation

  val StringValueFieldOffset: Long = TheUnsafe.objectFieldOffset(classOf[String].getDeclaredField("value"))
  val StringOffsetFieldOffset: Option[Long] = Try(TheUnsafe.objectFieldOffset(classOf[String].getDeclaredField("offset"))).toOption
  val StringCountFieldOffset: Option[Long] = Try(TheUnsafe.objectFieldOffset(classOf[String].getDeclaredField("count"))).toOption
  val StringImplementation: StringImplementation =
    StringOffsetFieldOffset match {
      case None => DirectCharsBasedStringImplementation
      case _    => OffsetBasedStringImplementation
    }

  val eol = System.getProperty("line.separator")

  val emptyFastring = fast""

  def bytes2Hex(bytes: Array[Byte]): String = bytes.foldLeft(new JStringBuilder(bytes.length)) { (buff, b) =>
    val shifted = b & 0xff
    if (shifted < 0x10)
      buff.append("0")
    buff.append(JLong.toString(shifted.toLong, 16))
  }.toString

  val StringCharsExtractor: String => Array[Char] = StringImplementation match {

    case DirectCharsBasedStringImplementation =>

      string => TheUnsafe.getObject(string, StringValueFieldOffset).asInstanceOf[Array[Char]]

      case OffsetBasedStringImplementation =>
      string => {
        val value = TheUnsafe.getObject(string, StringValueFieldOffset).asInstanceOf[Array[Char]]
        val offset = TheUnsafe.getInt(string, StringOffsetFieldOffset.get)
        val count = TheUnsafe.getInt(string, StringCountFieldOffset.get)

        if (offset == 0 && count == value.length)
          // no need to copy
          value
        else
          string.toCharArray
      }
  }

  object RichString {

    val EnsureTrimmedCharsArrayF: String => String = StringImplementation match {
      case DirectCharsBasedStringImplementation => identity[String]
      case _                                    => new String(_)
    }
  }

  implicit class RichString(val string: String) extends AnyVal {

    def clean = {
      val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
      normalized.toLowerCase.replaceAll("\\p{InCombiningDiacriticalMarks}+", "-").replaceAll("[^a-zA-Z0-9\\-]", "-")
    }

    def escapeJsDoubleQuoteString = string.replace("\"", "\\\"")

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

    def unsafeChars: Array[Char] = StringCharsExtractor(string)

    def ensureTrimmedCharsArray: String = RichString.EnsureTrimmedCharsArrayF(string)
  }
}
