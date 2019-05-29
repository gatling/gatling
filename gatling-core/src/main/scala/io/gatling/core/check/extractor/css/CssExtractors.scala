/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

class CssFindExtractor[X: NodeConverter](query: String, nodeAttribute: Option[String], occurrence: Int, selectors: CssSelectors)
  extends FindCriterionExtractor[NodeSelector, (String, Option[String]), X](
    "css",
    (query, nodeAttribute),
    occurrence,
    selectors.extractAll(_, (query, nodeAttribute)).lift(occurrence).success
  )

class CssFindAllExtractor[X: NodeConverter](query: String, nodeAttribute: Option[String], selectors: CssSelectors)
  extends FindAllCriterionExtractor[NodeSelector, (String, Option[String]), X](
    "css",
    (query, nodeAttribute),
    selectors.extractAll(_, (query, nodeAttribute)).liftSeqOption.success
  )

class CssCountExtractor(query: String, nodeAttribute: Option[String], selectors: CssSelectors)
  extends CountCriterionExtractor[NodeSelector, (String, Option[String])](
    "css",
    (query, nodeAttribute),
    prepared => Some(selectors.extractAll[String](prepared, (query, nodeAttribute)).size).success
  )
