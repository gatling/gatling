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
package io.gatling.core.check.extractor.regex

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._

class RegexExtractorFactory(implicit patterns: Patterns) extends CriterionExtractorFactory[CharSequence, String]("regex") {

  implicit def defaultSingleExtractor[X: GroupExtractor] = new SingleExtractor[CharSequence, String, X] {

    def extract(prepared: CharSequence, criterion: String, occurrence: Int): Validation[Option[X]] = {
      val matcher = patterns.compilePattern(criterion).matcher(prepared)
      matcher.findMatchN(occurrence).success
    }
  }

  implicit def defaultMultipleExtractor[X: GroupExtractor] = new MultipleExtractor[CharSequence, String, X] {
    def extract(prepared: CharSequence, criterion: String): Validation[Option[Seq[X]]] =
      patterns.extractAll(prepared, criterion).liftSeqOption.success
  }

  implicit val defaultCountExtractor = new CountExtractor[CharSequence, String] {
    def extract(prepared: CharSequence, criterion: String): Validation[Option[Int]] = {
      val matcher = patterns.compilePattern(criterion).matcher(prepared)

      var count = 0
      while (matcher.find)
        count = count + 1

      Some(count).success
    }
  }
}
