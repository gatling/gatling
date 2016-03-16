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


package io.gatling.http.check.async.message

import io.gatling.core.check._
import io.gatling.core.check.extractor.jsonpath._
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.Expression
import io.gatling.http.check.async.{ AsyncCheck, AsyncMessage }

trait AsyncMessageJsonPathOfType {
  self: AsyncMessageJsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter](implicit extractorFactory: JsonPathExtractorFactory) = new AsyncMessageJsonPathCheckBuilder[X](path, extender, jsonParsers)
}

object AsyncMessageJsonPathCheckBuilder {

  def preparer(jsonParsers: JsonParsers): Preparer[AsyncMessage, Any] =
    message => jsonParsers.safeParseBoon(message.string)

  def jsonPath(path: Expression[String], extender: Extender[AsyncCheck, AsyncMessage])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
    new AsyncMessageJsonPathCheckBuilder[String](path, extender, jsonParsers) with AsyncMessageJsonPathOfType
}

class AsyncMessageJsonPathCheckBuilder[X: JsonFilter](
  private[async] val path:        Expression[String],
  private[async] val extender:    Extender[AsyncCheck, AsyncMessage],
  private[async] val jsonParsers: JsonParsers
)(implicit extractorFactory: JsonPathExtractorFactory)
    extends DefaultMultipleFindCheckBuilder[AsyncCheck, AsyncMessage, Any, X](
      extender,
      AsyncMessageJsonPathCheckBuilder.preparer(jsonParsers)
    ) {

  import extractorFactory._

  def findExtractor(occurrence: Int) = path.map(newSingleExtractor[X](_, occurrence))
  def findAllExtractor = path.map(newMultipleExtractor[X])
  def countExtractor = path.map(newCountExtractor)
}
