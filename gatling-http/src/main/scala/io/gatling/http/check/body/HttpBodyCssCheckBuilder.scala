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

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.check.Preparer
import io.gatling.core.check.extractor.css.{ JoddCssExtractors, JsoupCssExtractors }
import io.gatling.core.config.{ Jodd, Jsoup }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.check.{ HttpCheckBuilders, HttpMultipleCheckBuilder }
import io.gatling.http.response.Response

object HttpBodyCssCheckBuilder extends Logging {

	object HttpBodyJoddCssCheckBuilder {

		import jodd.lagarto.dom.NodeSelector

		val preparer: Preparer[Response, NodeSelector] = (response: Response) =>
			try {
				JoddCssExtractors.parse(response.getResponseBody(configuration.core.encoding)).success

			} catch {
				case e: Exception =>
					val message = s"Could not parse response into a Jsoup Document: ${e.getMessage}"
					logger.info(message, e)
					message.failure
			}

		def css(expression: Expression[String], nodeAttribute: Option[String]) = new HttpMultipleCheckBuilder[NodeSelector, String, String](
			HttpCheckBuilders.bodyCheckFactory,
			preparer,
			JoddCssExtractors.extractOne(nodeAttribute),
			JoddCssExtractors.extractMultiple(nodeAttribute),
			JoddCssExtractors.count(nodeAttribute),
			expression)
	}

	object HttpBodyJsoupCssCheckBuilder {

		import org.jsoup.nodes.Document

		val preparer: Preparer[Response, Document] = (response: Response) =>
			try {
				JsoupCssExtractors.parse(response.getResponseBody(configuration.core.encoding)).success

			} catch {
				case e: Exception =>
					val message = s"Could not parse response into a Jsoup Document: ${e.getMessage}"
					logger.info(message, e)
					message.failure
			}

		def css(expression: Expression[String], nodeAttribute: Option[String]) = new HttpMultipleCheckBuilder[Document, String, String](
			HttpCheckBuilders.bodyCheckFactory,
			preparer,
			JsoupCssExtractors.extractOne(nodeAttribute),
			JsoupCssExtractors.extractMultiple(nodeAttribute),
			JsoupCssExtractors.count(nodeAttribute),
			expression)
	}

	def css(expression: Expression[String], nodeAttribute: Option[String]): HttpMultipleCheckBuilder[_, String, String] =
		configuration.core.extract.css.engine match {
			case Jodd => HttpBodyJoddCssCheckBuilder.css(expression, nodeAttribute)
			case Jsoup => HttpBodyJsoupCssCheckBuilder.css(expression, nodeAttribute)
		}
}