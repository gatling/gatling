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

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.session.Session

/**
 * This object groups all utilities for strings
 */
object StringHelper extends Logging {

	val END_OF_LINE = System.getProperty("line.separator")

	val EL_START = "${"

	val EL_END = "}"

	val EMPTY = ""

	val INDEX_START = "("

	val INDEX_END = ")"

	val jdk6Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

	val elPatternString = """\$\{([^\$]*)\}"""
	val elPattern = elPatternString.r
	val elMultivaluedPattern = """\((\d+)\)""".r

	/**
	 * Method that strips all accents from a string
	 */
	def stripAccents(string: String) = {
		val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
		jdk6Pattern.matcher(normalized).replaceAll(EMPTY);
	}

	def interpolate(stringToFormat: String): Session => String = {

		val keysFunctions = elPattern.findAllIn(stringToFormat).matchData.map { data =>
			val elContent = data.group(1)
			val multivaluedPart = elMultivaluedPattern.findFirstMatchIn(elContent)

			multivaluedPart match {
				case Some(multivaluedPartMatch) => {
					val key = elContent.substring(0, elContent.lastIndexOf(INDEX_START))
					(session: Session) => session.getAttribute(key).asInstanceOf[List[String]](multivaluedPartMatch.group(1).toInt)
				}
				case None => (session: Session) =>
					session.getAttribute[Any](data.group(1)) match {
						case list: List[_] => list(0).toString
						case str: String => str
						case x => x.toString
					}
			}
		}.toSeq

		if (keysFunctions.isEmpty) {
			// no interpolation
			(s: Session) => stringToFormat

		} else {
			val strings = stringToFormat.split(elPatternString, -1).toSeq

			val functions = keysFunctions zip strings

			(s: Session) => functions.map { entry => entry._2 + entry._1(s) }.mkString + strings.last
		}
	}
}