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

package io.gatling.core.check.extractor.regex

import io.gatling.core.check.DefaultMultipleFindCheckBuilder
import io.gatling.core.check.extractor.Extractor
import io.gatling.core.session.{ Expression, RichExpression }

trait RegexCheckType

trait RegexOfType { self: RegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor] = new RegexCheckBuilder[X](pattern, patterns)
}

object RegexCheckBuilder {

  def regex(pattern: Expression[String], patterns: Patterns) =
    new RegexCheckBuilder[String](pattern, patterns) with RegexOfType
}

class RegexCheckBuilder[X: GroupExtractor](
    private[regex] val pattern:  Expression[String],
    private[regex] val patterns: Patterns
)
  extends DefaultMultipleFindCheckBuilder[RegexCheckType, CharSequence, X](displayActualValue = true) {

  import RegexExtractorFactory._

  override def findExtractor(occurrence: Int): Expression[Extractor[CharSequence, X]] = pattern.map(newRegexSingleExtractor[X](_, occurrence, patterns))
  override def findAllExtractor: Expression[Extractor[CharSequence, Seq[X]]] = pattern.map(newRegexMultipleExtractor[X](_, patterns))
  override def countExtractor: Expression[Extractor[CharSequence, Int]] = pattern.map(newRegexCountExtractor(_, patterns))
}
