/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import io.gatling.core.check.{ CheckMaterializer, Preparer, Specializer }
import io.gatling.core.check.extractor.jsonpath.JsonPathCheckType
import io.gatling.core.json.JsonParsers
import io.gatling.http.check.{ HttpCheck, HttpCheckBuilders }
import io.gatling.http.response._

object HttpBodyJsonPathCheckMaterializer {

  private val CharsParsingThreshold = 200 * 1000

  private[body] val JoddResponseBodyUsageStrategy = new ResponseBodyUsageStrategy {
    override def bodyUsage(contentLength: Int): ResponseBodyUsage =
      if (contentLength <= CharsParsingThreshold) {
        StringResponseBodyUsage
      } else {
        InputStreamResponseBodyUsage
      }
  }

  private[body] val JacksonResponseBodyUsageStrategy = new ResponseBodyUsageStrategy {
    override def bodyUsage(contentLength: Int): ResponseBodyUsage =
      InputStreamResponseBodyUsage
  }

  private def jsonPathPreparer(jsonParsers: JsonParsers): Preparer[Response, Any] =
    response =>
      if (response.bodyLength > CharsParsingThreshold || jsonParsers.preferJackson)
        jsonParsers.safeParseJackson(response.body.stream, response.charset)
      else
        jsonParsers.safeParse(response.body.string)
}

class HttpBodyJsonPathCheckMaterializer(jsonParsers: JsonParsers) extends CheckMaterializer[JsonPathCheckType, HttpCheck, Response, Any] {

  import HttpBodyJsonPathCheckMaterializer._

  override val specializer: Specializer[HttpCheck, Response] = {
    val responseBodyUsageStrategy =
      if (jsonParsers.preferJackson) JacksonResponseBodyUsageStrategy
      else JoddResponseBodyUsageStrategy
    HttpCheckBuilders.bodySpecializer(responseBodyUsageStrategy)
  }

  override val preparer: Preparer[Response, Any] = jsonPathPreparer(jsonParsers)
}
