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

import scala.collection.JavaConversions.asScalaConcurrentMap
import scala.collection.mutable.ConcurrentMap

import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.NumberHelper.isNumeric

import grizzled.slf4j.Logging
/**
 * This object groups all utilities for strings
 */
object StringHelper extends Logging {

	val CACHE: ConcurrentMap[String, EvaluatableString] = new ConcurrentHashMap[String, EvaluatableString]

	val END_OF_LINE = System.getProperty("line.separator")

	val EL_START = "${"

	val EL_END = "}"

	val EMPTY = ""

	val INDEX_START = "("

	val INDEX_END = ")"

	val jdk6Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

	val elPattern = """\$\{(.+?)\}""".r
	val elOccurrencePattern = """(.+?)\((.+)\)""".r

	/**
	 * Method that strips all accents from a string
	 */
	def stripAccents(string: String) = {
		val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
		jdk6Pattern.matcher(normalized).replaceAll(EMPTY);
	}

	def parseEvaluatable(stringToFormat: String): EvaluatableString = {

		def parseStaticParts: Array[String] = elPattern.pattern.split(stringToFormat, -1)

		def parseDynamicParts: List[Session => Any] = {
			elPattern.findAllIn(stringToFormat).matchData.map { data =>
				val elContent = data.group(1)
				elOccurrencePattern.findFirstMatchIn(elContent) match {
					case Some(occurrencePartMatch) =>
						val key = occurrencePartMatch.group(1)
						val occurrence = occurrencePartMatch.group(2)
						val occurrenceFunction =
							if (isNumeric(occurrence))
								(session: Session) => Some(occurrence.toInt)
							else
								(session: Session) => session.getAttributeAsOption(occurrence)

						(session: Session) => {
							occurrenceFunction(session) match {
								case Some(resolvedOccurrence) => session.getAttributeAsOption[Seq[Any]](key) match {
									case Some(seq) if (seq.isDefinedAt(resolvedOccurrence)) => seq(resolvedOccurrence)
									case _ => {
										warn(StringBuilder.newBuilder.append("Couldn't resolve occurrence ").append(resolvedOccurrence).append(" of session multivalued attribute ").append(key))
										EMPTY
									}
								}
								case None => {
									warn("Couldn't resolve index session attribute " + occurrence)
									EMPTY
								}
							}
						}
					case None =>
						val key = data.group(1)
						(session: Session) => session.getAttributeAsOption[Any](key) match {
							case Some(x) => x
							case None => {
								warn("Couldn't resolve session attribute " + key)
								EMPTY
							}
						}
				}
			}.toList
		}

		def doParseEvaluatable: EvaluatableString = {
			val dynamicParts = parseDynamicParts

			if (dynamicParts.isEmpty) {
				// no interpolation
				(session: Session) => stringToFormat

			} else {
				val staticParts = parseStaticParts

				val functions = dynamicParts.zip(staticParts)

				(session: Session) => {
					val buffer = new StringBuilder

					functions.foreach { case (dynamicPart, staticPart) => buffer.append(staticPart).append(dynamicPart(session)) }

					buffer.append(staticParts.last).toString
				}
			}
		}

		CACHE.getOrElseUpdate(stringToFormat, doParseEvaluatable)
	}
}
