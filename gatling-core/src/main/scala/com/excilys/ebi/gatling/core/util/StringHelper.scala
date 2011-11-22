/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.util
import org.slf4j.helpers.MessageFormatter
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging
import java.util.regex.Pattern
import java.text.Normalizer
import akka.actor.Uuid

/**
 * This object groups all utilities for strings
 */
object StringHelper extends Logging {

	val EMPTY = ""

	val jdk6Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

	val elPatternString = """\$\{(.*?)\}"""
	val elPattern = elPatternString.r

	/**
	 * Method that strips all accents from a string
	 */
	def stripAccents(string: String) = {
		val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
		jdk6Pattern.matcher(normalized).replaceAll(EMPTY);
	}

	def interpolate(stringToFormat: String): Context => String = {
		val keysFunctions = elPattern.findAllIn(stringToFormat).matchData.map { data => (c: Context) => c.getAttribute(data.group(1)) }.toSeq
		val strings = stringToFormat.split(elPatternString, -1).toSeq

		val functions = keysFunctions zip strings

		(c: Context) => functions.map { entry => entry._2 + entry._1(c).toString }.mkString + strings.last
	}
}