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

import io.gatling.core.util.CacheHelper

import scala.collection.JavaConversions.asScalaBuffer

import io.gatling.core.check.extractor._
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import jodd.csselly.{ CSSelly, CssSelector }
import jodd.lagarto.dom.NodeSelector
import jodd.log.LoggerFactory
import jodd.log.impl.Slf4jLoggerFactory

object CssExtractor {

  LoggerFactory.setLoggerFactory(new Slf4jLoggerFactory)

  val DomBuilder = Jodd.newLagartoDomBuilder

  lazy val Cache = CacheHelper.newCache[String, JList[JList[CssSelector]]](configuration.core.extract.css.cacheMaxCapacity)

  def cached(query: String) =
    if (configuration.core.extract.css.cacheMaxCapacity > 0) Cache.getOrElseUpdate(query, CSSelly.parse(query))
    else CSSelly.parse(query)

  def parse(chars: Array[Char]) = new NodeSelector(DomBuilder.parse(chars))
  def parse(string: String) = new NodeSelector(DomBuilder.parse(string))
}

abstract class CssExtractor[X] extends CriterionExtractor[NodeSelector, String, X] {

  val criterionName = "css"

  def extractAll(selector: NodeSelector, query: String, nodeAttribute: Option[String]): Seq[String] = {

    val selectors = CssExtractor.cached(query)

    selector.select(selectors).flatMap { node =>
      nodeAttribute match {
        case Some(attr) => Option(node.getAttribute(attr))
        case _          => Some(node.getTextContent.trim)
      }
    }
  }
}

class SingleCssExtractor[X](val criterion: String, nodeAttribute: Option[String], val occurrence: Int) extends CssExtractor[String] with FindArity {

  def extract(prepared: NodeSelector): Validation[Option[String]] =
    extractAll(prepared, criterion, nodeAttribute).lift(occurrence).success
}

class MultipleCssExtractor[X](val criterion: String, nodeAttribute: Option[String]) extends CssExtractor[Seq[String]] with FindAllArity {

  def extract(prepared: NodeSelector): Validation[Option[Seq[String]]] =
    extractAll(prepared, criterion, nodeAttribute).liftSeqOption.success
}

class CountCssExtractor(val criterion: String, nodeAttribute: Option[String]) extends CssExtractor[Int] with CountArity {

  def extract(prepared: NodeSelector): Validation[Option[Int]] = {
    val count = extractAll(prepared, criterion, nodeAttribute).size
    Some(count).success
  }
}
