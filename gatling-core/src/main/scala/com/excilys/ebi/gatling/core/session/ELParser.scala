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

import com.excilys.ebi.gatling.core.util.NumberHelper.isNumeric

import grizzled.slf4j.Logging

object ELParser extends Logging {

	val elPattern = """\$\{(.+?)\}""".r
	val elSizePattern = """(.+?)\.size\(\)""".r
	val elOccurrencePattern = """(.+?)\((.+)\)""".r

	def parseEL(elString: String): EvaluatableString = {

		def parseStaticParts: Array[String] = elPattern.pattern.split(elString, -1)

		def parseDynamicParts: Seq[Session => Any] = elPattern
			.findAllIn(elString)
			.matchData
			.map {
				_.group(1) match {
					case elOccurrencePattern(key, occurrence) => {
						val occurrenceFunction =
							if (isNumeric(occurrence)) (session: Session) => Some(occurrence.toInt)
							else (session: Session) => session.getAttributeAsOption(occurrence)

						(session: Session) =>
							(for {
								resolvedOccurrence <- occurrenceFunction(session)
								attr <- session.getAttributeAsOption[Any](key)
								if (attr.isInstanceOf[Seq[_]])
								seq = attr.asInstanceOf[Seq[_]]
								if seq.isDefinedAt(resolvedOccurrence)
							} yield seq(resolvedOccurrence)).getOrElse { warn("Couldn't resolve EL " + elString); "" }
					}
					case elSizePattern(key) => {
						(session: Session) =>
							(for {
								attr <- session.getAttributeAsOption[Any](key)
								if (attr.isInstanceOf[Seq[_]])
								seq = attr.asInstanceOf[Seq[_]]
							} yield seq.size).getOrElse { warn("Couldn't resolve EL " + elString); 0 }

					}
					case key => (session: Session) => session.getAttributeAsOption[Any](key).getOrElse { warn("Couldn't resolve EL " + elString); "" }
				}
			}.toSeq

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
}