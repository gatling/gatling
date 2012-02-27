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
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

import scala.annotation.implicitNotFound
import scala.collection.JavaConverters.asScalaConcurrentMapConverter
import scala.collection.mutable.ConcurrentMap

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.core.session.Session

/**
 * This object groups all utilities for strings
 */
object StringHelper extends Logging {

	val CACHE: ConcurrentMap[String, EvaluatableString] = new ConcurrentHashMap[String, EvaluatableString].asScala

	val END_OF_LINE = System.getProperty("line.separator")

	val EL_START = "${"

	val EL_END = "}"

	val EMPTY = ""

	val INDEX_START = "("

	val INDEX_END = ")"

	val MISSING_SESSION_ATTRIBUTE = "undefined"

	val jdk6Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

	val elPatternString = """\$\{(.+?)\}"""
	val elPattern = elPatternString.r
	val elOccurrencePattern = """\((\d+)\)""".r

	/**
	 * Method that strips all accents from a string
	 */
	def stripAccents(string: String) = {
		val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
		jdk6Pattern.matcher(normalized).replaceAll(EMPTY);
	}

	def parseEvaluatable(stringToFormat: String): EvaluatableString = {

		def parseStaticParts: Array[String] = stringToFormat.split(elPatternString, -1)

		def parseDynamicParts: List[Session => Any] = {
			elPattern.findAllIn(stringToFormat).matchData.map { data =>
				val elContent = data.group(1)
				val occurrencePart = elOccurrencePattern.findFirstMatchIn(elContent)

				occurrencePart match {
					case Some(occurrencePartMatch) =>
						val key = elContent.substring(0, elContent.lastIndexOf(INDEX_START))
						val occurrence = occurrencePartMatch.group(1).toInt
						(session: Session) => session.getAttributeAsOption[Seq[Any]](key) match {
							case Some(x) if (x.size > occurrence) => x(occurrence)
							case _ => {
								logger.error("Couldn't resolve occurrence {} of session multivalued attribute {}", occurrence, key)
								MISSING_SESSION_ATTRIBUTE
							}
						}
					case None =>
						val key = data.group(1)
						(session: Session) => session.getAttributeAsOption[Any](key) match {
							case Some(x) => x
							case None => {
								logger.error("Couldn't resolve session attribute {}", key)
								MISSING_SESSION_ATTRIBUTE
							}
						}
				}
			}.toList
		}

		def doParseEvaluatable: EvaluatableString = {
			val dynamicParts = parseDynamicParts

			if (dynamicParts.isEmpty) {
				// no interpolation
				(s: Session) => stringToFormat

			} else {
				val staticParts = parseStaticParts

				val functions = dynamicParts.zip(staticParts)

				(s: Session) => {
					val buffer = new StringBuilder

					functions.foreach { entry =>
						buffer.append(entry._2).append(entry._1(s))
					}

					buffer.append(staticParts.last).toString
				}
			}
		}

		CACHE.getOrElseUpdate(stringToFormat, doParseEvaluatable)
	}
}
