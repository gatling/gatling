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
package com.excilys.ebi.gatling.core.session

import com.excilys.ebi.gatling.core.util.StringHelper
import com.excilys.ebi.gatling.core.util.NumberHelper.isNumeric
import scala.collection.mutable
import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

object ELParser extends Logging {

	val elPattern = """\$\{(.+?)\}""".r
	val elOccurrencePattern = """(.+?)\((.+)\)""".r

	val CACHE = mutable.Map.empty[String, EvaluatableString]

	def parseEL(elString: String): EvaluatableString = {

		def parseStaticParts: Array[String] = elPattern.pattern.split(elString, -1)

		def parseDynamicParts: Seq[Session => Any] = elPattern
			.findAllIn(elString)
			.matchData
			.map { data =>
				val elContent = data.group(1)
				elOccurrencePattern.findFirstMatchIn(elContent)
					.map { occurrencePartMatch =>
						val key = occurrencePartMatch.group(1)
						val occurrence = occurrencePartMatch.group(2)
						val occurrenceFunction =
							if (isNumeric(occurrence)) (session: Session) => Some(occurrence.toInt)
							else (session: Session) => session.getAttributeAsOption(occurrence)

						(session: Session) => occurrenceFunction(session)
							.map { resolvedOccurrence =>
								session.getAttributeAsOption[Seq[Any]](key) match {
									case Some(seq) if (seq.isDefinedAt(resolvedOccurrence)) => seq(resolvedOccurrence)
									case _ => warn("Couldn't resolve occurrence " + resolvedOccurrence + " of session multivalued attribute " + key); EMPTY
								}

							}.getOrElse { warn("Couldn't resolve index session attribute " + occurrence); EMPTY }

					}.getOrElse {
						val key = data.group(1)
						(session: Session) => session.getAttributeAsOption[Any](key).getOrElse { warn("Couldn't resolve session attribute " + key); EMPTY }
					}
			}.toSeq

		def doParseEvaluatable: EvaluatableString = {
			val dynamicParts = parseDynamicParts

			if (dynamicParts.isEmpty) {
				// no interpolation
				(session: Session) => elString

			} else {
				val staticParts = parseStaticParts

				val functions = dynamicParts.zip(staticParts)

				(session: Session) => functions
					.foldLeft(new StringBuilder) { (buffer, function) =>
						val (dynamicPart, staticPart) = function
						buffer.append(staticPart).append(dynamicPart(session))
					}.append(staticParts.last)
					.toString
			}
		}

		CACHE.getOrElseUpdate(elString, doParseEvaluatable)
	}
}