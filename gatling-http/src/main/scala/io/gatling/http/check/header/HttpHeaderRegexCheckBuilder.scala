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

import io.gatling.core.check._
import io.gatling.core.check.extractor.regex.{ GroupExtractor, Patterns }
import io.gatling.core.session.{ Expression, RichExpression, Session }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait HttpHeaderRegexCheckType

trait HttpHeaderRegexOfType {
  self: HttpHeaderRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor] = new HttpHeaderRegexCheckBuilder[X](headerName, pattern, patterns)
}

object HttpHeaderRegexCheckBuilder {

  def headerRegex(headerName: Expression[String], pattern: Expression[String], patterns: Patterns) =
    new HttpHeaderRegexCheckBuilder[String](headerName, pattern, patterns) with HttpHeaderRegexOfType
}

class HttpHeaderRegexCheckBuilder[X: GroupExtractor](
  private[header] val headerName: Expression[String],
  private[header] val pattern:    Expression[String],
  private[header] val patterns:   Patterns
)
    extends DefaultMultipleFindCheckBuilder[HttpHeaderRegexCheckType, Response, X] {

  private val headerAndPattern = (session: Session) => for {
    headerName <- headerName(session)
    pattern <- pattern(session)
  } yield (headerName, pattern)

  private val extractorFactory = new HttpHeaderRegexExtractorFactory(patterns)
  import extractorFactory._

  override def findExtractor(occurrence: Int) = headerAndPattern.map(newSingleExtractor[X](_, occurrence))
  override def findAllExtractor = headerAndPattern.map(newMultipleExtractor[X])
  override def countExtractor = headerAndPattern.map(newCountExtractor)
}

object HttpHeaderRegexProvider extends CheckProtocolProvider[HttpHeaderRegexCheckType, HttpCheck, Response, Response] {

  override val extender: Extender[HttpCheck, Response] = HeaderExtender

  override val preparer: Preparer[Response, Response] = PassThroughResponsePreparer
}
