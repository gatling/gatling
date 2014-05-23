/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check.header

import io.gatling.core.check.DefaultMultipleFindCheckBuilder
import io.gatling.core.check.extractor.regex.GroupExtractor
import io.gatling.core.session.{ Expression, RichExpression, Session }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait HttpHeaderRegexOfType {
  self: HttpHeaderRegexCheckBuilder[String] =>

  def ofType[X](implicit groupExtractor: GroupExtractor[X]) = new HttpHeaderRegexCheckBuilder[X](headerName, pattern)
}

object HttpHeaderRegexCheckBuilder {

  def headerRegex(headerName: Expression[String], pattern: Expression[String]) =
    new HttpHeaderRegexCheckBuilder[String](headerName, pattern) with HttpHeaderRegexOfType
}

class HttpHeaderRegexCheckBuilder[X](private[header] val headerName: Expression[String], val pattern: Expression[String])(implicit groupExtractor: GroupExtractor[X])
    extends DefaultMultipleFindCheckBuilder[HttpCheck, Response, Response, String](
      HeaderCheckFactory,
      PassThroughResponsePreparer) {

  val headerAndPattern = (session: Session) => for {
    headerName <- headerName(session)
    pattern <- pattern(session)
  } yield (headerName, pattern)

  def findExtractor(occurrence: Int) = headerAndPattern.map(new SingleHttpHeaderRegexExtractor[String](_, occurrence))

  def findAllExtractor = headerAndPattern.map(new MultipleHttpHeaderRegexExtractor[String](_))

  def countExtractor = headerAndPattern.map(new CountHttpHeaderRegexExtractor(_))
}
