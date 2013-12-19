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
package io.gatling.http.check.body

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.check.Preparer
import io.gatling.core.check.extractor.css.{ CountCssExtractor, CssExtractor, MultipleCssExtractor, SingleCssExtractor }
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.check.{ HttpCheckBuilders, HttpMultipleCheckBuilder }
import io.gatling.http.response.Response
import jodd.lagarto.dom.NodeSelector

object HttpBodyCssCheckBuilder extends StrictLogging {

	val preparer: Preparer[Response, NodeSelector] = (response: Response) =>
		try {
			CssExtractor.parse(response.chars).success

		} catch {
			case e: Exception =>
				val message = s"Could not parse response into a Jodd NodeSelector: ${e.getMessage}"
				logger.info(message, e)
				message.failure
		}

	def css(expression: Expression[String], nodeAttribute: Option[String]) =
		new HttpMultipleCheckBuilder[NodeSelector, String](HttpCheckBuilders.bodyCheckFactory, preparer) {
			def findExtractor(occurrence: Int) = expression.map(new SingleCssExtractor(_, nodeAttribute, occurrence))
			def findAllExtractor = expression.map(new MultipleCssExtractor(_, nodeAttribute))
			def countExtractor = expression.map(new CountCssExtractor(_, nodeAttribute))
		}
}
