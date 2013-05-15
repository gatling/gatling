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

import com.excilys.ebi.gatling.core.check.extractor.Extractor

abstract class CssExtractor extends Extractor {

	/**
	 * @param expression a String containing the CSS selector
	 * @return an option containing the value if found, None otherwise
	 */
	def extractOne(occurrence: Int, nodeAttribute: Option[String])(expression: String): Option[String] = extractMultiple(nodeAttribute)(expression) match {
		case Some(results) if (results.isDefinedAt(occurrence)) => results(occurrence)
		case _ => None
	}

	/**
	 * @param expression a String containing the CSS selector
	 * @param nodeAttribute specify an attribute if you don't want to extract the text content
	 * @return an option containing the values if found, None otherwise
	 */
	def extractMultiple(nodeAttribute: Option[String])(expression: String): Option[Seq[String]]

	/**
	 * @param expression a String containing the CSS selector
	 * @return an option containing the number of values if found, None otherwise
	 */
	def count(nodeAttribute: Option[String])(expression: String): Option[Int] = extractMultiple(nodeAttribute)(expression).map(_.size)
}