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
package io.gatling.http.check.ws

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, CheckFactory, Preparer }
import io.gatling.core.util.StringHelper.{ stringImplementation, DirectCharsBasedStringImplementation }
import io.gatling.core.check.extractor.jsonpath._
import io.gatling.http.check.body.HttpBodyJsonPathCheckBuilder.handleParseException
import io.gatling.core.session.Expression

trait WebSocketJsonPathOfType {
  self: WebSocketJsonPathCheckBuilder[String] =>

  def ofType[X](implicit jsonFilter: JsonFilter[X]) = new WebSocketJsonPathCheckBuilder[X](path, checkFactory)
}

object WebSocketJsonPathCheckBuilder {

  val preparer: Preparer[String, Any] = stringImplementation match {
    case DirectCharsBasedStringImplementation => handleParseException(BoonParser.parse)
    case _                                    => handleParseException(JacksonParser.parse)
  }

  def jsonPath(path: Expression[String], checkFactory: CheckFactory[WsCheck, String]) =
    new WebSocketJsonPathCheckBuilder[String](path, checkFactory) with WebSocketJsonPathOfType
}

class WebSocketJsonPathCheckBuilder[X](private[ws] val path: Expression[String],
                                       private[ws] val checkFactory: CheckFactory[WsCheck, String])(implicit jsonFilter: JsonFilter[X])
    extends DefaultMultipleFindCheckBuilder[WsCheck, String, Any, X](
      checkFactory,
      WebSocketJsonPathCheckBuilder.preparer) {

  def findExtractor(occurrence: Int) = path.map(new SingleJsonPathExtractor(_, occurrence))

  def findAllExtractor = path.map(new MultipleJsonPathExtractor(_))

  def countExtractor = path.map(new CountJsonPathExtractor(_))
}
