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

package io.gatling.http.check.url

import io.gatling.core.check.{ Extractor, _ }
import io.gatling.core.check.regex._
import io.gatling.core.session._
import io.gatling.http.check.{ HttpCheck, HttpCheckMaterializer }
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.check.HttpCheckScope.Url
import io.gatling.http.response.Response

trait CurrentLocationRegexCheckType

trait CurrentLocationRegexOfType {
  self: CurrentLocationRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor]: MultipleFindCheckBuilder[CurrentLocationRegexCheckType, String, X] = new CurrentLocationRegexCheckBuilder[X](pattern, patterns)
}

object CurrentLocationRegexCheckBuilder {

  def currentLocationRegex(pattern: Expression[String], patterns: Patterns): CurrentLocationRegexCheckBuilder[String] with CurrentLocationRegexOfType =
    new CurrentLocationRegexCheckBuilder[String](pattern, patterns) with CurrentLocationRegexOfType
}

class CurrentLocationRegexCheckBuilder[X: GroupExtractor](
    private[url] val pattern: Expression[String],
    private[url] val patterns: Patterns
) extends DefaultMultipleFindCheckBuilder[CurrentLocationRegexCheckType, String, X](displayActualValue = true) {

  override protected def findExtractor(occurrence: Int): Expression[Extractor[String, X]] =
    pattern.map(RegexExtractors.find[X]("currentLocationRegex", _, occurrence, patterns))
  override protected def findAllExtractor: Expression[Extractor[String, Seq[X]]] = pattern.map(RegexExtractors.findAll[X]("currentLocationRegex", _, patterns))
  override protected def countExtractor: Expression[Extractor[String, Int]] = pattern.map(RegexExtractors.count("currentLocationRegex", _, patterns))
}

object CurrentLocationRegexCheckMaterializer {

  val Instance: CheckMaterializer[CurrentLocationRegexCheckType, HttpCheck, Response, String] =
    new HttpCheckMaterializer[CurrentLocationRegexCheckType, String](Url, UrlStringPreparer)
}
