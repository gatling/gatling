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
package com.excilys.ebi.gatling.core.check.extractor.css

import scala.collection.JavaConversions.asScalaBuffer

import org.jsoup.Jsoup

/**
 * A built-in extractor for extracting values with Css Selectors
 *
 * @constructor creates a new CssExtractor
 * @param text the text where the search will be made
 */
class JsoupCssExtractor(string: String) extends CssExtractor {

	val document = Jsoup.parse(string, "")

	def extractMultiple(nodeAttribute: Option[String])(expression: String): Option[Seq[String]] = document
		.select(expression)
		.map { element =>
			nodeAttribute.map(element.attr(_)).getOrElse(element.text)
		}
}