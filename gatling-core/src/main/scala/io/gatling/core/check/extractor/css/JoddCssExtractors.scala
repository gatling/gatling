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

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import jodd.lagarto.dom.{ LagartoDOMBuilder, Node, NodeSelector }

object JoddCssExtractors {

	abstract class JoddCssExtractor[X] extends Extractor[NodeSelector, String, X] {
		val name = "css"
	}

	def parse(string: String) = new NodeSelector(new LagartoDOMBuilder().parse(string))

	private def extractAll(selector: NodeSelector, expression: String, nodeAttribute: Option[String]): Seq[String] = selector
		.select(expression)
		.map { node =>
			nodeAttribute.map(node.getAttribute).getOrElse(node.getTextContent.trim)
		}

	val extractOne = (nodeAttribute: Option[String]) => (occurrence: Int) => new JoddCssExtractor[String] {

		def apply(prepared: NodeSelector, criterion: String): Validation[Option[String]] =
			extractAll(prepared, criterion, nodeAttribute).lift(occurrence).success
	}

	val extractMultiple = (nodeAttribute: Option[String]) => new JoddCssExtractor[Seq[String]] {

		def apply(prepared: NodeSelector, criterion: String): Validation[Option[Seq[String]]] =
			extractAll(prepared, criterion, nodeAttribute).liftSeqOption.success
	}

	val count = (nodeAttribute: Option[String]) => new JoddCssExtractor[Int] {

		def apply(prepared: NodeSelector, criterion: String): Validation[Option[Int]] =
			extractAll(prepared, criterion, nodeAttribute).liftSeqOption.map(_.size).success
	}
}