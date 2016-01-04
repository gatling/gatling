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
package io.gatling.http.check.body

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.check.extractor.jsonpath._
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.http.check.{ HttpCheck, HttpCheckBuilders }
import io.gatling.http.response.{ InputStreamResponseBodyUsage, Response, ResponseBodyUsageStrategy, StringResponseBodyUsage }

trait HttpBodyJsonPathOfType {
  self: HttpBodyJsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter](implicit extractorFactory: JsonPathExtractorFactory) = new HttpBodyJsonPathCheckBuilder[X](path, jsonParsers)
}

object HttpBodyJsonPathCheckBuilder {

  val CharsParsingThreshold = 200 * 1000

  def preparer(jsonParsers: JsonParsers): Preparer[Response, Any] =
    response => {
      if (response.bodyLength > CharsParsingThreshold || jsonParsers.preferJackson)
        jsonParsers.safeParseJackson(response.body.stream, response.charset)
      else
        jsonParsers.safeParseBoon(response.body.string)
    }

  val BoonResponseBodyUsageStrategy = new ResponseBodyUsageStrategy {
    def bodyUsage(bodyLength: Int) =
      if (bodyLength <= CharsParsingThreshold)
        StringResponseBodyUsage
      else
        InputStreamResponseBodyUsage
  }

  val JacksonResponseBodyUsageStrategy = new ResponseBodyUsageStrategy {
    def bodyUsage(bodyLength: Int) =
      InputStreamResponseBodyUsage
  }

  def responseBodyUsageStrategy(jsonParsers: JsonParsers) =
    if (jsonParsers.preferJackson) JacksonResponseBodyUsageStrategy
    else BoonResponseBodyUsageStrategy

  def jsonPath(path: Expression[String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
    new HttpBodyJsonPathCheckBuilder[String](path, jsonParsers) with HttpBodyJsonPathOfType
}

class HttpBodyJsonPathCheckBuilder[X: JsonFilter](
  private[body] val path:        Expression[String],
  private[body] val jsonParsers: JsonParsers
)(implicit extractorFactory: JsonPathExtractorFactory)
    extends DefaultMultipleFindCheckBuilder[HttpCheck, Response, Any, X](
      HttpCheckBuilders.bodyExtender(HttpBodyJsonPathCheckBuilder.responseBodyUsageStrategy(jsonParsers)),
      HttpBodyJsonPathCheckBuilder.preparer(jsonParsers)
    ) {

  import extractorFactory._

  def findExtractor(occurrence: Int) = path.map(newSingleExtractor[X](_, occurrence))
  def findAllExtractor = path.map(newMultipleExtractor[X])
  def countExtractor = path.map(newCountExtractor)
}
