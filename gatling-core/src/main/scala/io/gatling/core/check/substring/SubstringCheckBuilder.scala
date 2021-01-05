/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check.substring

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Extractor }
import io.gatling.core.session.Expression

trait SubstringCheckType

class SubstringCheckBuilder(substring: Expression[String]) extends DefaultMultipleFindCheckBuilder[SubstringCheckType, String, Int](displayActualValue = true) {
  override protected def findExtractor(occurrence: Int): Expression[Extractor[String, Int]] = substring.map(SubstringExtractors.find(_, occurrence))
  override protected def findAllExtractor: Expression[Extractor[String, Seq[Int]]] = substring.map(SubstringExtractors.findAll)
  override protected def countExtractor: Expression[Extractor[String, Int]] = substring.map(SubstringExtractors.count)
}
