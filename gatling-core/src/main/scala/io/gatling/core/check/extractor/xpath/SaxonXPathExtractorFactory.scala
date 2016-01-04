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
import net.sf.saxon.s9api.XdmNode

class SaxonXPathExtractorFactory(implicit val saxon: Saxon) extends CriterionExtractorFactory[Option[XdmNode], (String, List[(String, String)])]("xpath") {

  implicit def defaultSingleExtractor = new SingleExtractor[Option[XdmNode], (String, List[(String, String)]), String] {
    override def extract(prepared: Option[XdmNode], criterion: (String, List[(String, String)]), occurrence: Int): Validation[Option[String]] = {
      val (path, namespaces) = criterion
      val result = for {
        text <- prepared
        xdmValue = saxon.evaluateXPath(path, namespaces, text)
        result <- if (occurrence < xdmValue.size) Some(xdmValue.itemAt(occurrence)) else None
      } yield result.getStringValue

      result.success
    }
  }

  implicit def defaultMultipleExtractor = new MultipleExtractor[Option[XdmNode], (String, List[(String, String)]), String] {
    override def extract(prepared: Option[XdmNode], criterion: (String, List[(String, String)])): Validation[Option[Seq[String]]] = {
      val (path, namespaces) = criterion
      val result = for {
        node <- prepared
        xdmValue = saxon.evaluateXPath(path, namespaces, node)
        // beware: we use toVector because xdmValue is an Iterable, so the Scala wrapper is a Stream
        // we don't want it to lazy load and hold a reference to the underlying DOM
        items <- if (xdmValue.size == 0) None else Some(xdmValue.map(_.getStringValue).toVector)
      } yield items

      result.success
    }
  }

  implicit val defaultCountExtractor = new CountExtractor[Option[XdmNode], (String, List[(String, String)])] {
    override def extract(prepared: Option[XdmNode], criterion: (String, List[(String, String)])): Validation[Option[Int]] = {
      val (path, namespaces) = criterion
      val count = prepared.map(saxon.evaluateXPath(path, namespaces, _).size).getOrElse(0)
      Some(count).success
    }
  }
}
