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

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._

import org.w3c.dom.Document

class JdkXPathExtractorFactory(implicit val jdkXmlParsers: JdkXmlParsers) extends CriterionExtractorFactory[Option[Document], (String, List[(String, String)])]("xpath") {

  implicit def defaultSingleExtractor = new SingleExtractor[Option[Document], (String, List[(String, String)]), String] {
    override def extract(prepared: Option[Document], criterion: (String, List[(String, String)]), occurrence: Int): Validation[Option[String]] = {
      val (path, namespaces) = criterion
      prepared.flatMap(document => jdkXmlParsers.extractAll(document, path, namespaces).lift(occurrence)).success
    }
  }

  implicit def defaultMultipleExtractor = new MultipleExtractor[Option[Document], (String, List[(String, String)]), String] {
    override def extract(prepared: Option[Document], criterion: (String, List[(String, String)])): Validation[Option[Seq[String]]] = {
      val (path, namespaces) = criterion
      prepared.flatMap(document => jdkXmlParsers.extractAll(document, path, namespaces).liftSeqOption).success
    }
  }

  implicit val defaultCountExtractor = new CountExtractor[Option[Document], (String, List[(String, String)])] {
    override def extract(prepared: Option[Document], criterion: (String, List[(String, String)])): Validation[Option[Int]] = {
      val (path, namespaces) = criterion
      prepared.map(document => jdkXmlParsers.nodeList(document, path, namespaces).getLength).success
    }
  }
}
