/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.check.ws

import io.gatling.core.check._
import io.gatling.core.session._
import io.gatling.core.check.extractor.regex._
import io.gatling.http.check.ws.WsCheckBuilders._

trait WsRegexOfType {
  self: WsRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor](implicit extractorFactory: RegexExtractorFactory) = new WsRegexCheckBuilder[X](expression, extender)
}

object WsRegexCheckBuilder {

  def regex(expression: Expression[String], extender: Extender[WsCheck, String])(implicit extractorFactory: RegexExtractorFactory) =
    new WsRegexCheckBuilder[String](expression, extender) with WsRegexOfType
}

class WsRegexCheckBuilder[X: GroupExtractor](private[ws] val expression: Expression[String],
                                             private[ws] val extender: Extender[WsCheck, String])(implicit extractorFactory: RegexExtractorFactory)
    extends DefaultMultipleFindCheckBuilder[WsCheck, String, CharSequence, X](
      extender,
      PassThroughMessagePreparer) {

  import extractorFactory._

  def findExtractor(occurrence: Int) = expression.map(newSingleExtractor[X](_, occurrence))
  def findAllExtractor = expression.map(newMultipleExtractor[X])
  def countExtractor = expression.map(newCountExtractor)
}
