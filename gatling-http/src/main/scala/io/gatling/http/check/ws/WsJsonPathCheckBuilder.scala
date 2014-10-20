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

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Extender, Preparer }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.json.{ Jackson, Boon }
import io.gatling.core.check.extractor.jsonpath._
import io.gatling.http.check.body.HttpBodyJsonPathCheckBuilder.handleParseException
import io.gatling.core.session.Expression

trait WsJsonPathOfType {
  self: WsJsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter] = new WsJsonPathCheckBuilder[X](path, extender)
}

object WsJsonPathCheckBuilder {

  val WsJsonPathPreparer: Preparer[String, Any] =
    if (configuration.core.extract.jsonPath.preferJackson) handleParseException(Jackson.parse)
    else handleParseException(Boon.parse)

  def jsonPath(path: Expression[String], extender: Extender[WsCheck, String]) =
    new WsJsonPathCheckBuilder[String](path, extender) with WsJsonPathOfType
}

class WsJsonPathCheckBuilder[X: JsonFilter](private[ws] val path: Expression[String],
                                            private[ws] val extender: Extender[WsCheck, String])
    extends DefaultMultipleFindCheckBuilder[WsCheck, String, Any, X](
      extender,
      WsJsonPathCheckBuilder.WsJsonPathPreparer) {

  def findExtractor(occurrence: Int) = path.map(new SingleJsonPathExtractor(_, occurrence))

  def findAllExtractor = path.map(new MultipleJsonPathExtractor(_))

  def countExtractor = path.map(new CountJsonPathExtractor(_))
}
