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

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import org.jboss.netty.util.internal.ConcurrentHashMap

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import jodd.csselly.CssSelector

object JoddCssExtractor {

	val cache: concurrent.Map[String, Seq[CssSelector]] = new ConcurrentHashMap[String, Seq[CssSelector]]

	def cached(query: String) = if (configuration.core.extract.css.cache) cache.getOrElseUpdate(query, ExtendedNodeSelector.parseQuery(query)) else ExtendedNodeSelector.parseQuery(query)

	def parse(string: String) = new ExtendedNodeSelector(new SilentLagartoDOMBuilder().parse(string))

	def extractAll(selector: ExtendedNodeSelector, query: String, nodeAttribute: Option[String]): Seq[String] = {

		val selectors = cached(query)

		selector.select(selectors).map { node =>
			nodeAttribute match {
				case Some(attr) => node.getAttribute(attr)
				case _ => node.getTextContent.trim
			}
		}
	}
}

abstract class JoddCssExtractor[X] extends CriterionExtractor[ExtendedNodeSelector, String, X] { val name = "css" }

class SingleJoddCssExtractor[X](val criterion: Expression[String], nodeAttribute: Option[String], occurrence: Int) extends JoddCssExtractor[String] {

	def extract(prepared: ExtendedNodeSelector, criterion: String): Validation[Option[String]] =
		JoddCssExtractor.extractAll(prepared, criterion, nodeAttribute).lift(occurrence).success
}

class MultipleJoddCssExtractor[X](val criterion: Expression[String], nodeAttribute: Option[String]) extends JoddCssExtractor[Seq[String]] {

	def extract(prepared: ExtendedNodeSelector, criterion: String): Validation[Option[Seq[String]]] =
		JoddCssExtractor.extractAll(prepared, criterion, nodeAttribute).liftSeqOption.success
}

class CountJoddCssExtractor(val criterion: Expression[String], nodeAttribute: Option[String]) extends JoddCssExtractor[Int] {

	def extract(prepared: ExtendedNodeSelector, criterion: String): Validation[Option[Int]] =
		JoddCssExtractor.extractAll(prepared, criterion, nodeAttribute).liftSeqOption.map(_.size).success
}
