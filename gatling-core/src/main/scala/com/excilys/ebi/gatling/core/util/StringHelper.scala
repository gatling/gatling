/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.util

import java.text.Normalizer
import java.util.regex.Pattern

import scala.collection.mutable

import com.excilys.ebi.gatling.core.session.{ EvaluatableString, EvaluatableStringSeq, Session }

import grizzled.slf4j.Logging

/**
 * This object groups all utilities for strings
 */
object StringHelper extends Logging {

	val END_OF_LINE = System.getProperty("line.separator")

	val EMPTY = ""

	val jdk6Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

	/**
	 * Method that strips all accents from a string
	 */
	def stripAccents(string: String) = {
		val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
		jdk6Pattern.matcher(normalized).replaceAll(EMPTY);
	}

	def escapeJsQuoteString(s: String) = s.replace("'", "\\\'")

	def bytes2Hex(bytes: Array[Byte]): String = bytes.foldLeft(new StringBuilder) { (buff, b) =>
		if ((b & 0xff) < 0x10)
			buff.append("0")
		buff.append(java.lang.Long.toString(b & 0xff, 16))
	}.toString

	def trimToOption(string: String) = string.trim match {
		case EMPTY => None
		case string => Some(string)
	}
}
