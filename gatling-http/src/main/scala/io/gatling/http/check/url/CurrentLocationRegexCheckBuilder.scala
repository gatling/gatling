/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.core.check.extractor.regex._
import io.gatling.core.session._
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait CurrentLocationRegexCheckType

trait CurrentLocationRegexOfType {
  self: CurrentLocationRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor] = new CurrentLocationRegexCheckBuilder[X](pattern, patterns)
}

object CurrentLocationRegexCheckBuilder {

  def currentLocationRegex(pattern: Expression[String], patterns: Patterns) =
    new CurrentLocationRegexCheckBuilder[String](pattern, patterns) with CurrentLocationRegexOfType

  private val ExtractorFactory = new RegexExtractorFactoryBase("currentLocationRegex")
}

class CurrentLocationRegexCheckBuilder[X: GroupExtractor](
    private[url] val pattern:  Expression[String],
    private[url] val patterns: Patterns
)
  extends DefaultMultipleFindCheckBuilder[CurrentLocationRegexCheckType, CharSequence, X](displayActualValue = true) {

  import CurrentLocationRegexCheckBuilder.ExtractorFactory._

  override def findExtractor(occurrence: Int) = pattern.map(newRegexSingleExtractor[X](_, occurrence, patterns))
  override def findAllExtractor = pattern.map(newRegexMultipleExtractor[X](_, patterns))
  override def countExtractor = pattern.map(newRegexCountExtractor(_, patterns))
}

object CurrentLocationRegexCheckMaterializer
  extends CheckMaterializer[CurrentLocationRegexCheckType, HttpCheck, Response, CharSequence] {

  override protected val specializer: Specializer[HttpCheck, Response] = UrlSpecializer

  override protected val preparer: Preparer[Response, String] = UrlStringPreparer
}
