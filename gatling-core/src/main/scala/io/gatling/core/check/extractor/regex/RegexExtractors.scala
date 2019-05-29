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

class RegexFindExtractor[X: GroupExtractor](name: String, pattern: String, occurrence: Int, patterns: Patterns)
  extends FindCriterionExtractor[CharSequence, String, X](
    name,
    pattern,
    occurrence,
    prepared => {
      val matcher = patterns.compilePattern(pattern).matcher(prepared)
      matcher.findMatchN(occurrence).success
    }
  )

class RegexFindAllExtractor[X: GroupExtractor](name: String, pattern: String, patterns: Patterns)
  extends FindAllCriterionExtractor[CharSequence, String, X](
    name,
    pattern,
    patterns.extractAll(_, pattern).liftSeqOption.success
  )

class RegexCountExtractor(name: String, pattern: String, patterns: Patterns)
  extends CountCriterionExtractor[CharSequence, String](
    name,
    pattern,
    prepared => {
      val matcher = patterns.compilePattern(pattern).matcher(prepared)

      var count = 0
      while (matcher.find)
        count = count + 1

      Some(count).success
    }
  )
