/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.check.async

import io.gatling.core.check.extractor.jsonpath._
import io.gatling.core.check.{ Extender, DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.Expression
import io.gatling.http.check.body.HttpBodyJsonpJsonPathCheckBuilder

trait AsyncJsonpJsonPathOfType {
  self: AsyncJsonpJsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter](implicit extractorFactory: JsonPathExtractorFactory) =
    new AsyncJsonpJsonPathCheckBuilder[X](path, extender, jsonParsers)
}

object AsyncJsonpJsonPathCheckBuilder {

  def asyncJsonpPreparer(jsonParsers: JsonParsers): Preparer[String, Any] = HttpBodyJsonpJsonPathCheckBuilder.parseJsonpString(_, jsonParsers)

  def jsonpJsonPath(path: Expression[String], extender: Extender[AsyncCheck, String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
    new AsyncJsonpJsonPathCheckBuilder[String](path, extender, jsonParsers) with AsyncJsonpJsonPathOfType
}

class AsyncJsonpJsonPathCheckBuilder[X: JsonFilter](
  private[async] val path:        Expression[String],
  private[async] val extender:    Extender[AsyncCheck, String],
  private[async] val jsonParsers: JsonParsers
)(implicit extractorFactory: JsonPathExtractorFactory)
    extends DefaultMultipleFindCheckBuilder[AsyncCheck, String, Any, X](extender, AsyncJsonpJsonPathCheckBuilder.asyncJsonpPreparer(jsonParsers)) {

  import extractorFactory._

  def findExtractor(occurrence: Int) = path.map(newSingleExtractor[X](_, occurrence))
  def findAllExtractor = path.map(newMultipleExtractor[X])
  def countExtractor = path.map(newCountExtractor)
}
