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

import io.gatling.core.check.DefaultMultipleFindCheckBuilder
import io.gatling.core.check.extractor.regex.{ RegexExtractorFactory, GroupExtractor }
import io.gatling.core.session.{ Expression, RichExpression, Session }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait HttpHeaderRegexOfType {
  self: HttpHeaderRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor](implicit extractorFactory: HttpHeaderRegexExtractorFactory) = new HttpHeaderRegexCheckBuilder[X](headerName, pattern)
}

object HttpHeaderRegexCheckBuilder {

  def headerRegex(headerName: Expression[String], pattern: Expression[String])(implicit extractorFactory: HttpHeaderRegexExtractorFactory) =
    new HttpHeaderRegexCheckBuilder[String](headerName, pattern) with HttpHeaderRegexOfType
}

class HttpHeaderRegexCheckBuilder[X: GroupExtractor](private[header] val headerName: Expression[String], val pattern: Expression[String])(implicit extractorFactory: HttpHeaderRegexExtractorFactory)
    extends DefaultMultipleFindCheckBuilder[HttpCheck, Response, Response, X](
      HeaderExtender,
      PassThroughResponsePreparer
    ) {

  val headerAndPattern = (session: Session) => for {
    headerName <- headerName(session)
    pattern <- pattern(session)
  } yield (headerName, pattern)

  import extractorFactory._

  def findExtractor(occurrence: Int) = headerAndPattern.map(newSingleExtractor[X](_, occurrence))
  def findAllExtractor = headerAndPattern.map(newMultipleExtractor[X])
  def countExtractor = headerAndPattern.map(newCountExtractor)
}
