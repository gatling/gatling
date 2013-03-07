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
package com.excilys.ebi.gatling.core.check.extractor.jsonpath

import java.io.InputStream

import scala.collection.JavaConversions.asScalaIterator

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{ JsonNode => JacksonNode, MappingJsonFactory, ObjectMapper }
import com.fasterxml.jackson.databind.node.{ ArrayNode, ObjectNode, ValueNode }

object Json {

	private def convert(name: String, node: JacksonNode): JsonNode = node match {
		case objectNode: ObjectNode =>
			val children = objectNode.fields.toIterable.flatMap { entry =>
				entry.getValue match {
					case array: ArrayNode => array.flatten(entry.getKey)
					case value => Iterable(convert(entry.getKey, value))
				}
			}
			JsonElement(name, children)

		case value: ValueNode => JsonText(name, value.asText)
	}

	private implicit class FlattenableArrayNode(val array: ArrayNode) extends AnyVal {
		def flatten(name: String): Iterable[JsonNode] = array.elements.map(convert(name, _)).toIterable
	}

	val mapper = {
		val jacksonFeatures = configuration.http.nonStandardJsonSupport.map(JsonParser.Feature.valueOf)
		val jsonFactory = new MappingJsonFactory
		jacksonFeatures.foreach(jsonFactory.enable)
		new ObjectMapper(jsonFactory)
	}

	def parse(is: InputStream): JsonNode = {

		val root = mapper.readValue(is, classOf[JacksonNode])
		root match {
			case array: ArrayNode => JsonElement("", array.flatten(""))
			case node => convert("", node)
		}
	}
}