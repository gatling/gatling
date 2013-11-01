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
package io.gatling.core.check.extractor.css

import scala.collection.JavaConversions.asScalaBuffer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedSeqOption }
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object JsoupCssExtractor {

	def parse(string: String) = Jsoup.parse(string, "")

	def extractAll(selector: Document, expression: String, nodeAttribute: Option[String]): Seq[String] =
		selector.select(expression)
			.map { element => nodeAttribute.map(element.attr).getOrElse(element.text) }
}

abstract class JsoupCssExtractor[X] extends CriterionExtractor[Document, String, X] {
	val name = "css"
}

class SingleJsoupCssExtractor(val criterion: Expression[String], nodeAttribute: Option[String], occurrence: Int) extends JsoupCssExtractor[String] {

	def extract(prepared: Document, criterion: String): Validation[Option[String]] =
		JsoupCssExtractor.extractAll(prepared, criterion, nodeAttribute).lift(occurrence).success
}

class MultipleJsoupCssExtractor(val criterion: Expression[String], nodeAttribute: Option[String]) extends JsoupCssExtractor[Seq[String]] {

	def extract(prepared: Document, criterion: String): Validation[Option[Seq[String]]] =
		JsoupCssExtractor.extractAll(prepared, criterion, nodeAttribute).liftSeqOption.success
}

class CountJsoupCssExtractor(val criterion: Expression[String], nodeAttribute: Option[String]) extends JsoupCssExtractor[Int] {

	def extract(prepared: Document, criterion: String): Validation[Option[Int]] =
		JsoupCssExtractor.extractAll(prepared, criterion, nodeAttribute).liftSeqOption.map(_.size).success
}
