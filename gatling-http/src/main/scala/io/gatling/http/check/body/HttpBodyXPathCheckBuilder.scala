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
import io.gatling.core.check.extractor.xpath.{ CountXPathExtractor, MultipleXPathExtractor, SingleXPathExtractor, XPathExtractor }
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.check.{ HttpCheckBuilders, HttpMultipleCheckBuilder }
import io.gatling.http.response.Response
import net.sf.saxon.s9api.XdmNode

object HttpBodyXPathCheckBuilder extends StrictLogging {

	val preparer: Preparer[Response, Option[XdmNode]] = (response: Response) =>
		try {
			val root = if (response.hasResponseBody) Some(XPathExtractor.parse(response.body.stream())) else None
			root.success

		} catch {
			case e: Exception =>
				val message = s"Could not parse response into a DOM Document: ${e.getMessage}"
				logger.info(message, e)
				message.failure
		}

	def xpath(expression: Expression[String], namespaces: List[(String, String)]) =
		new HttpMultipleCheckBuilder[Option[XdmNode], String](HttpCheckBuilders.streamBodyCheckFactory, preparer) {
			def findExtractor(occurrence: Int) = expression.map(new SingleXPathExtractor(_, namespaces, occurrence))
			def findAllExtractor = expression.map(new MultipleXPathExtractor(_, namespaces))
			def countExtractor = expression.map(new CountXPathExtractor(_, namespaces))
		}
}
