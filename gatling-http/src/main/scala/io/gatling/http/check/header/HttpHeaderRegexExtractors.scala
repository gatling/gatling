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

package io.gatling.http.check.header

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._
import io.gatling.core.check.extractor.regex.{ GroupExtractor, Patterns }
import io.gatling.http.response.Response

object HttpHeaderRegexExtractors {
  def extractHeadersValues[X: GroupExtractor](response: Response, headerName: String, pattern: String, patterns: Patterns): Seq[X] =
    response.headers(headerName).flatMap(patterns.extractAll(_, pattern))
}

class HttpHeaderRegexFindExtractor[X: GroupExtractor](headerName: String, pattern: String, occurrence: Int, patterns: Patterns)
  extends FindCriterionExtractor[Response, (String, String), X](
    "headerRegex",
    (headerName, pattern),
    occurrence,
    HttpHeaderRegexExtractors.extractHeadersValues(_, headerName, pattern, patterns).lift(occurrence).success
  )

class HttpHeaderRegexFindAllExtractor[X: GroupExtractor](headerName: String, pattern: String, patterns: Patterns)
  extends FindAllCriterionExtractor[Response, (String, String), X](
    "headerRegex",
    (headerName, pattern),
    HttpHeaderRegexExtractors.extractHeadersValues(_, headerName, pattern, patterns).liftSeqOption.success
  )

class HttpHeaderRegexCountExtractor(headerName: String, pattern: String, patterns: Patterns)
  extends CountCriterionExtractor[Response, (String, String)](
    "headerRegex",
    (headerName, pattern),
    HttpHeaderRegexExtractors.extractHeadersValues[String](_, headerName, pattern, patterns).liftSeqOption.map(_.size).success
  )
