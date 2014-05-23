/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

  def ofType[X](implicit groupExtractor: GroupExtractor[X]) = new WsRegexCheckBuilder[X](expression, checkFactory)
}

object WsRegexCheckBuilder {

  def regex(expression: Expression[String], checkFactory: CheckFactory[WsCheck, String]) =
    new WsRegexCheckBuilder[String](expression, checkFactory) with WsRegexOfType
}

class WsRegexCheckBuilder[X](private[ws] val expression: Expression[String],
                             private[ws] val checkFactory: CheckFactory[WsCheck, String])(implicit groupExtractor: GroupExtractor[X])
    extends DefaultMultipleFindCheckBuilder[WsCheck, String, CharSequence, X](
      checkFactory,
      PassThroughMessagePreparer) {

  def findExtractor(occurrence: Int) = expression.map(new SingleRegexExtractor(_, occurrence))

  def findAllExtractor = expression.map(new MultipleRegexExtractor(_))

  def countExtractor = expression.map(new CountRegexExtractor(_))
}
