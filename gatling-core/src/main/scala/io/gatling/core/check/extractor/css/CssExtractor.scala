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
package io.gatling.core.check.extractor.css

import java.util.{ List => JList }

import scala.collection.JavaConversions.{ asScalaBuffer, mapAsScalaConcurrentMap }
import scala.collection.concurrent

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import jodd.csselly.{ CSSelly, CssSelector }
import jodd.lagarto.dom.{ LagartoDOMBuilder, NodeSelector }
import jodd.log.LoggerFactory
import jodd.log.impl.Slf4jLoggerFactory
import jsr166e.ConcurrentHashMapV8

object CssExtractor {

	LoggerFactory.setLoggerFactory(new Slf4jLoggerFactory)

	val domBuilder = new LagartoDOMBuilder
	domBuilder.setParsingErrorLogLevelName("INFO")

	val cache: concurrent.Map[String, JList[JList[CssSelector]]] = new ConcurrentHashMapV8[String, JList[JList[CssSelector]]]

	def cached(query: String) = if (configuration.core.extract.css.cache) cache.getOrElseUpdate(query, CSSelly.parse(query)) else CSSelly.parse(query)

	def parse(chars: Array[Char]) = new NodeSelector(domBuilder.parse(chars))
	def parse(string: String) = new NodeSelector(domBuilder.parse(string))

	def extractAll(selector: NodeSelector, query: String, nodeAttribute: Option[String]): Seq[String] = {

		val selectors = cached(query)

		selector.select(selectors).flatMap { node =>
			nodeAttribute match {
				case Some(attr) => Option(node.getAttribute(attr))
				case _ => Some(node.getTextContent.trim)
			}
		}
	}
}

abstract class CssExtractor[X] extends CriterionExtractor[NodeSelector, String, X] { val criterionName = "css" }

class SingleCssExtractor[X](val criterion: String, nodeAttribute: Option[String], occurrence: Int) extends CssExtractor[String] {

	def extract(prepared: NodeSelector): Validation[Option[String]] =
		CssExtractor.extractAll(prepared, criterion, nodeAttribute).lift(occurrence).success
}

class MultipleCssExtractor[X](val criterion: String, nodeAttribute: Option[String]) extends CssExtractor[Seq[String]] {

	def extract(prepared: NodeSelector): Validation[Option[Seq[String]]] =
		CssExtractor.extractAll(prepared, criterion, nodeAttribute).liftSeqOption.success
}

class CountCssExtractor(val criterion: String, nodeAttribute: Option[String]) extends CssExtractor[Int] {

	def extract(prepared: NodeSelector): Validation[Option[Int]] = {
		val count = CssExtractor.extractAll(prepared, criterion, nodeAttribute).size
		Some(count).success
	}
}
