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

import io.gatling.core.check.extractor.jsonpath._
import io.gatling.core.check.{ Extender, DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.Expression
import io.gatling.http.check.body.HttpBodyJsonpJsonPathCheckBuilder

trait WsJsonpJsonPathOfType {
  self: WsJsonpJsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter](implicit extractorFactory: JsonPathExtractorFactory) =
    new WsJsonpJsonPathCheckBuilder[X](path, extender, jsonParsers)
}

object WsJsonpJsonPathCheckBuilder {

  def wsJsonpPreparer(jsonParsers: JsonParsers): Preparer[String, Any] = HttpBodyJsonpJsonPathCheckBuilder.parseJsonpString(_, jsonParsers)

  def jsonpJsonPath(path: Expression[String], extender: Extender[WsCheck, String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
    new WsJsonpJsonPathCheckBuilder[String](path, extender, jsonParsers) with WsJsonpJsonPathOfType
}

class WsJsonpJsonPathCheckBuilder[X: JsonFilter](private[ws] val path: Expression[String],
                                                 private[ws] val extender: Extender[WsCheck, String],
                                                 private[ws] val jsonParsers: JsonParsers)(implicit extractorFactory: JsonPathExtractorFactory)
    extends DefaultMultipleFindCheckBuilder[WsCheck, String, Any, X](extender, WsJsonpJsonPathCheckBuilder.wsJsonpPreparer(jsonParsers)) {

  import extractorFactory._

  def findExtractor(occurrence: Int) = path.map(newSingleExtractor[X](_, occurrence))
  def findAllExtractor = path.map(newMultipleExtractor[X])
  def countExtractor = path.map(newCountExtractor)
}
