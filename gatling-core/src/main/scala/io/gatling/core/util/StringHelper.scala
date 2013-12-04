/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.nio.charset.Charset
import java.text.Normalizer

import com.dongxiguo.fastring.Fastring.Implicits._

/**
 * This object groups all utilities for strings
 */
object StringHelper {

	val stringCopyChars = UnsafeHelper.unsafe.isDefined && UnsafeHelper.stringCountFieldOffset == -1L

	val eol = System.getProperty("line.separator")

	val emptyFastring = fast""

	def bytes2Hex(bytes: Array[Byte]): String = bytes.foldLeft(new StringBuilder) { (buff, b) =>
		if ((b & 0xff) < 0x10)
			buff.append("0")
		buff.append(java.lang.Long.toString(b & 0xff, 16))
	}.toString

	def ensureCharCopy(value: String) =
		if (stringCopyChars) value
		else new String(value)

	implicit class RichString(val string: String) extends AnyVal {

		def clean = {
			val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
			normalized.toLowerCase.replaceAll("\\p{InCombiningDiacriticalMarks}+", "-").replaceAll("[^a-zA-Z0-9\\-]", "-")
		}

		def escapeJsDoubleQuoteString = string.replace("\"", "\\\"")

		def trimToOption = string.trim match {
			case "" => None
			case string => Some(string)
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
	}
}
