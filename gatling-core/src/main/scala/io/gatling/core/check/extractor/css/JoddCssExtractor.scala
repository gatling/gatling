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

import io.gatling.core.check.CriterionExtractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import jodd.lagarto.dom.NodeSelector

object JoddCssExtractor {

	def parse(string: String) = new NodeSelector(new SilentLagartoDOMBuilder().parse(string))

	def extractAll(selector: NodeSelector, expression: String, nodeAttribute: Option[String]): Seq[String] = selector
		.select(expression)
		.map { node =>
			nodeAttribute.map(node.getAttribute).getOrElse(node.getTextContent.trim)
		}
}

abstract class JoddCssExtractor[X] extends CriterionExtractor[NodeSelector, String, X] {
	val name = "css"
}

class SingleJoddCssExtractor[X](val criterion: Expression[String], nodeAttribute: Option[String], occurrence: Int) extends JoddCssExtractor[String] {

	def extract(prepared: NodeSelector, criterion: String): Validation[Option[String]] =
		JoddCssExtractor.extractAll(prepared, criterion, nodeAttribute).lift(occurrence).success
}

class MultipleJoddCssExtractor[X](val criterion: Expression[String], nodeAttribute: Option[String]) extends JoddCssExtractor[Seq[String]] {

	def extract(prepared: NodeSelector, criterion: String): Validation[Option[Seq[String]]] =
		JoddCssExtractor.extractAll(prepared, criterion, nodeAttribute).liftSeqOption.success
}

class CountJoddCssExtractor(val criterion: Expression[String], nodeAttribute: Option[String]) extends JoddCssExtractor[Int] {

	def extract(prepared: NodeSelector, criterion: String): Validation[Option[Int]] =
		JoddCssExtractor.extractAll(prepared, criterion, nodeAttribute).liftSeqOption.map(_.size).success
}
