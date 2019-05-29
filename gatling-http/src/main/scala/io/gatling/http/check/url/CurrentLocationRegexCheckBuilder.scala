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

package io.gatling.http.check.url

import io.gatling.core.check._
import io.gatling.core.check.extractor.Extractor
import io.gatling.core.check.extractor.regex._
import io.gatling.core.session._
import io.gatling.http.check.HttpCheckMaterializer
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.check.HttpCheckScope.Url
import io.gatling.http.response.Response

trait CurrentLocationRegexCheckType

trait CurrentLocationRegexOfType {
  self: CurrentLocationRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor] = new CurrentLocationRegexCheckBuilder[X](pattern, patterns)
}

object CurrentLocationRegexCheckBuilder {

  def currentLocationRegex(pattern: Expression[String], patterns: Patterns) =
    new CurrentLocationRegexCheckBuilder[String](pattern, patterns) with CurrentLocationRegexOfType
}

class CurrentLocationRegexCheckBuilder[X: GroupExtractor](
    private[url] val pattern:  Expression[String],
    private[url] val patterns: Patterns
)
  extends DefaultMultipleFindCheckBuilder[CurrentLocationRegexCheckType, CharSequence, X](displayActualValue = true) {

  override def findExtractor(occurrence: Int): Expression[Extractor[CharSequence, X]] = pattern.map(new RegexFindExtractor[X]("currentLocationRegex", _, occurrence, patterns))
  override def findAllExtractor: Expression[Extractor[CharSequence, Seq[X]]] = pattern.map(new RegexFindAllExtractor[X]("currentLocationRegex", _, patterns))
  override def countExtractor: Expression[Extractor[CharSequence, Int]] = pattern.map(new RegexCountExtractor("currentLocationRegex", _, patterns))
}

object CurrentLocationRegexCheckMaterializer extends HttpCheckMaterializer[CurrentLocationRegexCheckType, CharSequence](Url) {

  override protected val preparer: Preparer[Response, String] = UrlStringPreparer
}
