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
package io.gatling.core.check.extractor.css

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._

import jodd.lagarto.dom.NodeSelector

object CssExtractorFactory extends CriterionExtractorFactory[NodeSelector, (String, Option[String])]("css") {

  def newCssSingleExtractor[X: NodeConverter](query: String, nodeAttribute: Option[String], occurrence: Int, selectors: CssSelectors) =
    newSingleExtractor(
      (query, nodeAttribute),
      occurrence,
      selectors.extractAll(_, (query, nodeAttribute)).lift(occurrence).success
    )

  def newCssMultipleExtractor[X: NodeConverter](query: String, nodeAttribute: Option[String], selectors: CssSelectors) =
    newMultipleExtractor(
      (query, nodeAttribute),
      selectors.extractAll(_, (query, nodeAttribute)).liftSeqOption.success
    )

  def newCssCountExtractor(query: String, nodeAttribute: Option[String], selectors: CssSelectors) =
    newCountExtractor(
      (query, nodeAttribute),
      prepared => Some(selectors.extractAll[String](prepared, (query, nodeAttribute)).size).success
    )
}
