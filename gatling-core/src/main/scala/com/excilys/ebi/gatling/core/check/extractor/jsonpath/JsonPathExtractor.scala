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
package com.excilys.ebi.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable

import com.excilys.ebi.gatling.core.check.extractor.Extractor
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.jayway.jsonpath.{ InvalidPathException, JsonPath }

import net.minidev.json.JSONArray
import net.minidev.json.parser.JSONParser

object JsonPathExtractor {

	val cache = mutable.Map.empty[String, JsonPath]
	def cachedJsonPath(expression: String): JsonPath =
		if (configuration.core.extract.jsonPath.cache) cache.getOrElseUpdate(expression, JsonPath.compile(expression))
		else JsonPath.compile(expression)
}

/**
 * A built-in extractor for extracting values with  Xpath like expressions for Json
 *
 * @constructor creates a new JsonPathExtractor
 * @param textContent the text where the search will be made
 */
class JsonPathExtractor(bytes: Array[Byte]) extends Extractor {

	import JsonPathExtractor._

	val json = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes)

	/**
	 * @param occurrence
	 * @param expression
	 * @return extract the occurrence of the given rank matching the expression
	 */
	def extractOne(occurrence: Int)(expression: String): Option[String] = extractMultiple(expression) match {
		case Some(results) if (results.isDefinedAt(occurrence)) => results(occurrence)
		case _ => None
	}

	/**
	 * @param expression
	 * @return extract all the occurrences matching the expression
	 */
	def extractMultiple(expression: String): Option[Seq[String]] = {

		try {
			cachedJsonPath(expression).read[Any](json) match {
				case null => None // can't turn result into an Option as we want to turn empty Seq into None (see below)
				case array: JSONArray => array.map(_.toString)
				case other => Some(List(other.toString))
			}
		} catch {
			case e: InvalidPathException => None
		}
	}

	/**
	 * @param expression
	 * @return count all the occurrences matching the expression
	 */
	def count(expression: String): Option[Int] = extractMultiple(expression).map(_.size)
}