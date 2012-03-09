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
package com.excilys.ebi.gatling.core.check.extractor.jsonpath
import scala.collection.JavaConverters.asScalaBufferConverter

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.JsonNode

import com.excilys.ebi.gatling.core.check.extractor.Extractor.{ toOption, seqToOption }

object JsonPathExtractor {
	lazy val MAPPER = new ObjectMapper
}

class JsonPathExtractor(textContent: String) {

	val json = JsonPathExtractor.MAPPER.readValue(textContent, classOf[JsonNode])

	def extractOne(occurrence: Int)(expression: String): Option[String] = extractMultiple(expression) match {
		case Some(results) if (results.length > occurrence) => results(occurrence)
		case _ => None
	}

	def extractMultiple(expression: String): Option[Seq[String]] = {
		val results = new JaxenJackson(expression).selectNodes(json).asScala.map(_.asInstanceOf[JsonNode].asText)
		results
	}

	def count(expression: String): Option[Int] = extractMultiple(expression).map(_.size)
}