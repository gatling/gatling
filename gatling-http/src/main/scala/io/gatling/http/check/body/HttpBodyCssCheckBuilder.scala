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

import java.nio.charset.Charset

import io.gatling.core.check.Preparer
import io.gatling.core.check.extractor.css.CssExtractors
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.check.{ HttpCheckBuilders, HttpMultipleCheckBuilder }
import io.gatling.http.response.ExtendedResponse

import grizzled.slf4j.Logging
import jodd.lagarto.dom.NodeSelector

object HttpBodyCssCheckBuilder extends Logging {

	private val preparer: Preparer[ExtendedResponse, NodeSelector] = (response: ExtendedResponse) =>
		try {
			val charBuffer = Charset.forName(configuration.simulation.encoding).decode(response.getResponseBodyAsByteBuffer)
			CssExtractors.parse(charBuffer).success

		} catch {
			case e: Exception =>
				val message = s"Could not parse response into a Jodd NodeSelector: ${e.getMessage}"
				info(message, e)
				message.failure
		}

	def css(expression: Expression[String], nodeAttribute: Option[String]) = new HttpMultipleCheckBuilder[NodeSelector, String, String](
		HttpCheckBuilders.bodyCheckFactory,
		preparer,
		CssExtractors.extractOne(nodeAttribute),
		CssExtractors.extractMultiple(nodeAttribute),
		CssExtractors.count(nodeAttribute),
		expression)
}