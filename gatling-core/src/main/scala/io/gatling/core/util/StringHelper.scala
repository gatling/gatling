/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.text.Normalizer
import java.util.regex.Pattern

import com.dongxiguo.fastring.Fastring.Implicits._

/**
 * This object groups all utilities for strings
 */
object StringHelper {

	val eol = System.getProperty("line.separator")

	val jdk6Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

	def bytes2Hex(bytes: Array[Byte]): String = bytes.foldLeft(new StringBuilder) { (buff, b) =>
		if ((b & 0xff) < 0x10)
			buff.append("0")
		buff.append(java.lang.Long.toString(b & 0xff, 16))
	}.toString

	implicit class RichString(val string: String) extends AnyVal {

		def stripAccents = {
			val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
			jdk6Pattern.matcher(normalized).replaceAll("_")
		}

		def escapeJsQuoteString = string.replace("'", "\\'")

		def escapeJsDoubleQuoteString = string.replace("\"", "\\\"")

		def trimToOption = string.trim match {
			case "" => None
			case string => Some(string)
		}

		def truncate(maxLength: Int) = if (string.length < maxLength) string else string.substring(0, maxLength) + "..."

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
