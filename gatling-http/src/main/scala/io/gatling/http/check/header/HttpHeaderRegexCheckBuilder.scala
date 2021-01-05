/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.core.check.regex.{ GroupExtractor, Patterns }
import io.gatling.core.session.Expression
import io.gatling.http.check.{ HttpCheck, HttpCheckMaterializer }
import io.gatling.http.check.HttpCheckScope.Header
import io.gatling.http.response.Response

trait HttpHeaderRegexCheckType

trait HttpHeaderRegexOfType {
  self: HttpHeaderRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor]: MultipleFindCheckBuilder[HttpHeaderRegexCheckType, Response, X] =
    new HttpHeaderRegexCheckBuilder[X](headerName, pattern, patterns)
}

object HttpHeaderRegexCheckBuilder {

  def headerRegex(
      headerName: Expression[CharSequence],
      pattern: Expression[String],
      patterns: Patterns
  ): HttpHeaderRegexCheckBuilder[String] with HttpHeaderRegexOfType =
    new HttpHeaderRegexCheckBuilder[String](headerName, pattern, patterns) with HttpHeaderRegexOfType
}

class HttpHeaderRegexCheckBuilder[X: GroupExtractor](
    private[header] val headerName: Expression[CharSequence],
    private[header] val pattern: Expression[String],
    private[header] val patterns: Patterns
) extends DefaultMultipleFindCheckBuilder[HttpHeaderRegexCheckType, Response, X](displayActualValue = true) {

  private def withHeaderAndPattern[T](f: (CharSequence, String) => T): Expression[T] =
    session =>
      for {
        headerName <- headerName(session)
        pattern <- pattern(session)
      } yield f(headerName, pattern)

  override protected def findExtractor(occurrence: Int): Expression[Extractor[Response, X]] =
    withHeaderAndPattern(HttpHeaderRegexExtractors.find(_, _, occurrence, patterns))

  override protected def findAllExtractor: Expression[Extractor[Response, Seq[X]]] = withHeaderAndPattern(HttpHeaderRegexExtractors.findAll(_, _, patterns))

  override protected def countExtractor: Expression[Extractor[Response, Int]] = withHeaderAndPattern(HttpHeaderRegexExtractors.count(_, _, patterns))
}

object HttpHeaderRegexCheckMaterializer {

  val Instance: CheckMaterializer[HttpHeaderRegexCheckType, HttpCheck, Response, Response] =
    new HttpCheckMaterializer[HttpHeaderRegexCheckType, Response](Header, identityPreparer)
}
