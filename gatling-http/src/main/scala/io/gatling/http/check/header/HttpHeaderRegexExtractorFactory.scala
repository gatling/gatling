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

object HttpHeaderRegexExtractorFactory extends CriterionExtractorFactory[Response, (String, String)]("headerRegex") {

  private def extractHeadersValues[X: GroupExtractor](response: Response, headerName: String, pattern: String, patterns: Patterns): Seq[X] =
    response.headers(headerName).flatMap(patterns.extractAll(_, pattern))

  def newHeaderRegexSingleExtractor[X: GroupExtractor](headerName: String, pattern: String, occurrence: Int, patterns: Patterns): CriterionExtractor[Response, (String, String), X] with FindArity =
    newSingleExtractor(
      (headerName, pattern),
      occurrence,
      extractHeadersValues(_, headerName, pattern, patterns).lift(occurrence).success
    )

  def newHeaderRegexMultipleExtractor[X: GroupExtractor](headerName: String, pattern: String, patterns: Patterns): CriterionExtractor[Response, (String, String), Seq[X]] with FindAllArity =
    newMultipleExtractor(
      (headerName, pattern),
      extractHeadersValues(_, headerName, pattern, patterns).liftSeqOption.success
    )

  def newHeaderRegexCountExtractor(headerName: String, pattern: String, patterns: Patterns): CriterionExtractor[Response, (String, String), Int] with CountArity =
    newCountExtractor(
      (headerName, pattern),
      extractHeadersValues[String](_, headerName, pattern, patterns).liftSeqOption.map(_.size).success
    )
}
