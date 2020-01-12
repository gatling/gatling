/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.core.check._

object RegexExtractors {

  def find[X: GroupExtractor](name: String, pattern: String, occurrence: Int, patterns: Patterns): FindCriterionExtractor[String, String, X] = {

    val compiledPattern = patterns.compilePattern(pattern)

    new FindCriterionExtractor[String, String, X](
      name,
      pattern,
      occurrence,
      prepared => {
        val matcher = compiledPattern.matcher(prepared)
        matcher.findMatchN(occurrence).success
      }
    )
  }

  def findAll[X: GroupExtractor](name: String, pattern: String, patterns: Patterns): FindAllCriterionExtractor[String, String, X] =
    new FindAllCriterionExtractor[String, String, X](
      name,
      pattern,
      patterns.extractAll(_, pattern).liftSeqOption.success
    )

  def count(name: String, pattern: String, patterns: Patterns): CountCriterionExtractor[String, String] = {

    val compiledPattern = patterns.compilePattern(pattern)

    new CountCriterionExtractor[String, String](
      name,
      pattern,
      prepared => {
        val matcher = compiledPattern.matcher(prepared)

        var count = 0
        while (matcher.find) count = count + 1

        Some(count).success
      }
    )
  }
}
