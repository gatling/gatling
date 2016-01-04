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
package io.gatling.http.check.header

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._
import io.gatling.core.check.extractor.regex.{ Patterns, GroupExtractor }
import io.gatling.http.response.Response

class HttpHeaderRegexExtractorFactory(implicit patterns: Patterns) extends CriterionExtractorFactory[Response, (String, String)]("headerRegex") {

  private def extractHeadersValues[X: GroupExtractor](response: Response, headerNameAndPattern: (String, String)) = {
    val (headerName, pattern) = headerNameAndPattern
    val headerValues = response.headers(headerName)
    headerValues.map(patterns.extractAll(_, pattern)).flatten
  }

  implicit def defaultSingleExtractor[X: GroupExtractor] = new SingleExtractor[Response, (String, String), X] {

    def extract(prepared: Response, criterion: (String, String), occurrence: Int): Validation[Option[X]] =
      extractHeadersValues(prepared, criterion).lift(occurrence).success
  }

  implicit def defaultMultipleExtractor[X: GroupExtractor] = new MultipleExtractor[Response, (String, String), X] {
    def extract(prepared: Response, criterion: (String, String)): Validation[Option[Seq[X]]] =
      extractHeadersValues(prepared, criterion).liftSeqOption.success
  }

  implicit val defaultCountExtractor = new CountExtractor[Response, (String, String)] {
    def extract(prepared: Response, criterion: (String, String)): Validation[Option[Int]] =
      extractHeadersValues[String](prepared, criterion).liftSeqOption.map(_.size).success
  }
}
