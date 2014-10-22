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

  val UnsupportedJavaVersion = new UnsupportedOperationException(s"Gatling requires Java >= 7u6, but running ${System.getProperty("java.version")}")
  UnsupportedJavaVersion.setStackTrace(new Array[StackTraceElement](0))

  private val StringValueFieldOffset: Long = TheUnsafe.objectFieldOffset(classOf[String].getDeclaredField("value"))
  private val StringOffsetFieldOffset: Option[Long] = Try(TheUnsafe.objectFieldOffset(classOf[String].getDeclaredField("offset"))).toOption

  def checkSupportedJavaVersion(): Unit = StringOffsetFieldOffset.foreach(throw UnsupportedJavaVersion)

  val Eol = System.getProperty("line.separator")

  val Crlf = "\r\n"

  val EmptyFastring = fast""

  def bytes2Hex(bytes: Array[Byte]): String = bytes.foldLeft(new JStringBuilder(bytes.length)) { (buff, b) =>
    val shifted = b & 0xff
    if (shifted < 0x10)
      buff.append("0")
    buff.append(JLong.toString(shifted.toLong, 16))
  }.toString

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

    def unsafeChars: Array[Char] = TheUnsafe.getObject(string, StringValueFieldOffset).asInstanceOf[Array[Char]]
  }
}
