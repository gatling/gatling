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
package io.gatling.core.check.extractor.jsonpath

import java.io.InputStream

import scala.collection.mutable

import io.gatling.core.config.GatlingConfiguration.configuration
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{ JsonNode => JacksonNode, MappingJsonFactory, ObjectMapper }
import com.fasterxml.jackson.databind.node.{ ArrayNode, ObjectNode, ValueNode }

object Json {

	private def convert(name: String, node: JacksonNode): JsonNode = node match {
		case objectNode: ObjectNode =>
			val children = new mutable.ArrayBuffer[JsonNode]
			val it = objectNode.fields
			while (it.hasNext) {
				val field = it.next
				field.getValue match {
					case array: ArrayNode => children.addChildren(field.getKey, array)
					case value => children += convert(field.getKey, value)
				}
			}

			JsonElement(name, children)

		case value: ValueNode => JsonText(name, value.asText)
	}

	private implicit class ChildrenNodesBuffer(val children: mutable.ArrayBuffer[JsonNode]) extends AnyVal {

		def addChildren(name: String, array: ArrayNode) {
			val it = array.elements
			while (it.hasNext) {
				children += convert(name, it.next)
			}
		}
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
			case array: ArrayNode =>
				val children = new mutable.ArrayBuffer[JsonNode]
				children.addChildren("_", array)
				JsonElement("", children)
			case node => convert("", node)
		}
	}
}