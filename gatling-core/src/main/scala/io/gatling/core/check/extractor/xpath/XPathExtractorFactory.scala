/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.collection.JavaConversions._

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._

class XPathExtractorFactory(saxon: Saxon, jdkXmlParsers: JdkXmlParsers) extends CriterionExtractorFactory[Option[Dom], (String, List[(String, String)])]("xpath") {

  implicit val defaultSingleExtractor = new SingleExtractor[Option[Dom], (String, List[(String, String)]), String] {
    override def extract(prepared: Option[Dom], criterion: (String, List[(String, String)]), occurrence: Int): Validation[Option[String]] = {
      val (path, namespaces) = criterion
      prepared.flatMap {
        case SaxonDom(document) =>
          val xdmValue = saxon.evaluateXPath(path, namespaces, document)
          if (occurrence < xdmValue.size)
            Some(xdmValue.itemAt(occurrence).getStringValue)
          else
            None

        case JdkDom(document) => jdkXmlParsers.extractAll(document, path, namespaces).lift(occurrence)
      }.success
    }
  }

  implicit val defaultMultipleExtractor = new MultipleExtractor[Option[Dom], (String, List[(String, String)]), String] {
    override def extract(prepared: Option[Dom], criterion: (String, List[(String, String)])): Validation[Option[Seq[String]]] = {
      val (path, namespaces) = criterion
      prepared.flatMap {
        case SaxonDom(document) =>
          val xdmValue = saxon.evaluateXPath(path, namespaces, document)

          if (xdmValue.nonEmpty)
            // beware: we use toVector because xdmValue is an Iterable, so the Scala wrapper is a Stream
            // we don't want it to lazy load and hold a reference to the underlying DOM
            Some(xdmValue.map(_.getStringValue).toVector)
          else
            None

        case JdkDom(document) => jdkXmlParsers.extractAll(document, path, namespaces).liftSeqOption
      }.success
    }
  }

  implicit val defaultCountExtractor = new CountExtractor[Option[Dom], (String, List[(String, String)])] {
    override def extract(prepared: Option[Dom], criterion: (String, List[(String, String)])): Validation[Option[Int]] = {
      val (path, namespaces) = criterion
      prepared.map {
        case SaxonDom(document) => saxon.evaluateXPath(path, namespaces, document).size

        case JdkDom(document)   => jdkXmlParsers.nodeList(document, path, namespaces).getLength
      }.orElse(Some(0))
        .success
    }
  }
}
