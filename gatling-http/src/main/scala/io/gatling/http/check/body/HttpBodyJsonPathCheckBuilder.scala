/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.check.Preparer
import io.gatling.core.check.extractor.jsonpath.{ BoonParser, CountJsonPathExtractor, JacksonParser, JsonFilter, MultipleJsonPathExtractor, SingleJsonPathExtractor }
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.util.JVMHelper
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.check.{ HttpCheckBuilders, HttpMultipleCheckBuilder }
import io.gatling.http.response.Response

object HttpBodyJsonPathCheckBuilder extends StrictLogging {

	def handleParseException(block: Response => Any) = (response: Response) =>
		try {
			block(response).success
		} catch {
			case e: Exception =>
				val message = s"Could not parse response into a JSON object: ${e.getMessage}"
				logger.info(message, e)
				message.failure
		}

	val preparer: Preparer[Response, Any] =
		if (JVMHelper.java6)
			handleParseException { response =>
				JacksonParser.parse(response.getResponseBodyAsBytes)
			}
		else
			handleParseException { response =>
				BoonParser.parse(response.getResponseBody)
			}

	def jsonPath[X](path: Expression[String])(implicit groupExtractor: JsonFilter[X]) =
		new HttpMultipleCheckBuilder[Any, X](HttpCheckBuilders.bodyCheckFactory, preparer) {
			def findExtractor(occurrence: Int) = path.map(new SingleJsonPathExtractor(_, occurrence))
			def findAllExtractor = path.map(new MultipleJsonPathExtractor(_))
			def countExtractor = path.map(new CountJsonPathExtractor(_))
		}
}
