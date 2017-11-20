/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.check.extractor.xpath

import scala.collection.JavaConverters._

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._

object XPathExtractorFactory extends CriterionExtractorFactory[Option[Dom], (String, List[(String, String)])]("xpath") {

  def newXpathSingleExtractor(path: String, namespaces: List[(String, String)], occurrence: Int, xmlParsers: XmlParsers) =
    newSingleExtractor(
      (path, namespaces),
      occurrence,
      _.flatMap {
        case SaxonDom(document) =>
          val xdmValue = xmlParsers.saxon.evaluateXPath(path, namespaces, document)
          if (occurrence < xdmValue.size)
            Some(xdmValue.itemAt(occurrence).getStringValue)
          else
            None

        case JdkDom(document) => xmlParsers.jdk.extractAll(document, path, namespaces).lift(occurrence)
      }.success
    )

  def newXpathMultipleExtractor(path: String, namespaces: List[(String, String)], xmlParsers: XmlParsers) =
    newMultipleExtractor(
      (path, namespaces),
      _.flatMap {
        case SaxonDom(document) =>
          val xdmValue = xmlParsers.saxon.evaluateXPath(path, namespaces, document).asScala

          if (xdmValue.nonEmpty)
            // beware: we use toVector because xdmValue is an Iterable, so the Scala wrapper is a Stream
            // we don't want it to lazy load and hold a reference to the underlying DOM
            Some(xdmValue.map(_.getStringValue).toVector)
          else
            None

        case JdkDom(document) => xmlParsers.jdk.extractAll(document, path, namespaces).liftSeqOption
      }.success
    )

  def newXpathCountExtractor(path: String, namespaces: List[(String, String)], xmlParsers: XmlParsers) =
    newCountExtractor(
      (path, namespaces),
      _.map {
        case SaxonDom(document) => xmlParsers.saxon.evaluateXPath(path, namespaces, document).size

        case JdkDom(document)   => xmlParsers.jdk.nodeList(document, path, namespaces).getLength
      }.orElse(Some(0))
        .success
    )
}
