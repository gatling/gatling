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

import com.fasterxml.jackson.core.JsonParser.Feature
import com.fasterxml.jackson.databind.ObjectMapper

import io.gatling.core.config.GatlingConfiguration.configuration

object JacksonParser extends JsonParser {

	val objectMapper = new ObjectMapper
	objectMapper.configure(Feature.ALLOW_COMMENTS, configuration.core.extract.jsonPath.jackson.allowComments)
	objectMapper.configure(Feature.ALLOW_SINGLE_QUOTES, configuration.core.extract.jsonPath.jackson.allowSingleQuotes)
	objectMapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, configuration.core.extract.jsonPath.jackson.allowUnquotedFieldNames)

	def parse(bytes: Array[Byte]) = objectMapper.readValue(bytes, classOf[Object])
	def parse(string: String) = objectMapper.readValue(string, classOf[Object])
}
