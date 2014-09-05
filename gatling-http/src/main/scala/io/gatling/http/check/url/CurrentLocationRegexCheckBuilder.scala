/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check.url

import io.gatling.core.check.DefaultMultipleFindCheckBuilder
import io.gatling.core.check.extractor.regex.{ CountRegexExtractor, MultipleRegexExtractor, SingleRegexExtractor, GroupExtractor }
import io.gatling.core.session._
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait CurrentLocationRegexOfType { self: CurrentLocationRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor] = new CurrentLocationRegexCheckBuilder[X](expression)
}

object CurrentLocationRegexCheckBuilder {

  def currentLocationRegex(expression: Expression[String]) = new CurrentLocationRegexCheckBuilder[String](expression) with CurrentLocationRegexOfType
}

/**
 * Gatling check builder that allows for validating request URLs
 * with regex patterns.
 *
 * @param expression
 * @tparam X
 */
class CurrentLocationRegexCheckBuilder[X: GroupExtractor](private[url] val expression: Expression[String])
    extends DefaultMultipleFindCheckBuilder[HttpCheck, Response, CharSequence, X](
      StringBodyExtender,
      UrlStringPreparer) {

  def findExtractor(occurrence: Int) = expression.map(new SingleRegexExtractor(_, occurrence))
  def findAllExtractor = expression.map(new MultipleRegexExtractor(_))
  def countExtractor = expression.map(new CountRegexExtractor(_))
}
