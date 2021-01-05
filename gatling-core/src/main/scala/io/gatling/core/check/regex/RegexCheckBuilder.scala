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

package io.gatling.core.check.regex

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Extractor, MultipleFindCheckBuilder }
import io.gatling.core.session.{ Expression, RichExpression }

trait RegexCheckType

trait RegexOfType { self: RegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor]: MultipleFindCheckBuilder[RegexCheckType, String, X] = new RegexCheckBuilder[X](pattern, patterns)
}

object RegexCheckBuilder {

  def regex(pattern: Expression[String], patterns: Patterns): RegexCheckBuilder[String] with RegexOfType =
    new RegexCheckBuilder[String](pattern, patterns) with RegexOfType
}

class RegexCheckBuilder[X: GroupExtractor](
    private[regex] val pattern: Expression[String],
    private[regex] val patterns: Patterns
) extends DefaultMultipleFindCheckBuilder[RegexCheckType, String, X](displayActualValue = true) {

  override protected def findExtractor(occurrence: Int): Expression[Extractor[String, X]] =
    pattern.map(RegexExtractors.find[X]("regex", _, occurrence, patterns))
  override protected def findAllExtractor: Expression[Extractor[String, Seq[X]]] = pattern.map(RegexExtractors.findAll[X]("regex", _, patterns))
  override protected def countExtractor: Expression[Extractor[String, Int]] = pattern.map(RegexExtractors.count("regex", _, patterns))
}
