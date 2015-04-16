/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.check.extractor.css

import io.gatling.core.check.extractor._
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import jodd.lagarto.dom.NodeSelector

class CssExtractorFactory(implicit val selectors: CssSelectors) extends CriterionExtractorFactory[NodeSelector, (String, Option[String])]("css") {

  implicit def defaultSingleExtractor = new SingleExtractor[NodeSelector, (String, Option[String]), String] {

    def extract(prepared: NodeSelector, criterion: (String, Option[String]), occurrence: Int): Validation[Option[String]] =
      selectors.extractAll(prepared, criterion).lift(occurrence).success
  }

  implicit def defaultMultipleExtractor = new MultipleExtractor[NodeSelector, (String, Option[String]), String] {
    def extract(prepared: NodeSelector, criterion: (String, Option[String])): Validation[Option[Seq[String]]] =
      selectors.extractAll(prepared, criterion).liftSeqOption.success
  }

  implicit val defaultCountExtractor = new CountExtractor[NodeSelector, (String, Option[String])] {
    def extract(prepared: NodeSelector, criterion: (String, Option[String])): Validation[Option[Int]] = {
      val count = selectors.extractAll(prepared, criterion).size
      Some(count).success
    }
  }
}
