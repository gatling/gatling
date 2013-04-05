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
package io.gatling.http.check.body

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.check.Preparer
import io.gatling.core.check.extractor.jsonpath.{ Json, JsonNode, JsonPathExtractors }
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.check.{ HttpCheckBuilders, HttpMultipleCheckBuilder }
import io.gatling.http.response.ExtendedResponse

object HttpBodyJsonPathCheckBuilder extends Logging {

	val preparer: Preparer[ExtendedResponse, Option[JsonNode]] = (response: ExtendedResponse) =>
		try {
			val json = if (response.hasResponseBody) Some(response.getResponseBodyAsStream) else None
			json.map(Json.parse).success

		} catch {
			case e: Exception =>
				val message = s"Could not parse response into a JSON tree: ${e.getMessage}"
				logger.info(message, e)
				message.failure
		}

	def jsonPath(expression: Expression[String]) = new HttpMultipleCheckBuilder[Option[JsonNode], String, String](
		HttpCheckBuilders.bodyCheckFactory,
		preparer,
		JsonPathExtractors.extractOne,
		JsonPathExtractors.extractMultiple,
		JsonPathExtractors.count,
		expression)
}
