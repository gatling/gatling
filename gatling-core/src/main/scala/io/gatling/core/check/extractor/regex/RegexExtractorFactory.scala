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

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._

class RegexExtractorFactoryBase(name: String) extends CriterionExtractorFactory[CharSequence, String](name) {

  def newRegexSingleExtractor[X: GroupExtractor](pattern: String, occurrence: Int, patterns: Patterns): CriterionExtractor[CharSequence, String, X] with FindArity =
    newSingleExtractor(
      pattern,
      occurrence,
      prepared => {
        val matcher = patterns.compilePattern(pattern).matcher(prepared)
        matcher.findMatchN(occurrence).success
      }
    )

  def newRegexMultipleExtractor[X: GroupExtractor](pattern: String, patterns: Patterns): CriterionExtractor[CharSequence, String, Seq[X]] with FindAllArity =
    newMultipleExtractor(
      pattern,
      patterns.extractAll(_, pattern).liftSeqOption.success
    )

  def newRegexCountExtractor(pattern: String, patterns: Patterns): CriterionExtractor[CharSequence, String, Int] with CountArity =
    newCountExtractor(
      pattern,
      prepared => {
        val matcher = patterns.compilePattern(pattern).matcher(prepared)

        var count = 0
        while (matcher.find)
          count = count + 1

        Some(count).success
      }
    )
}

object RegexExtractorFactory extends RegexExtractorFactoryBase("regex")
