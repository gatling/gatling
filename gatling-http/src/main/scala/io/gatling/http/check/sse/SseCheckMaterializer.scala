/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.http.check.sse

import io.gatling.core.check._
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.json.JsonParsers

import com.fasterxml.jackson.databind.JsonNode

final class SseCheckMaterializer[T, P](override val preparer: Preparer[String, P]) extends CheckMaterializer[T, SseCheck, String, P](new SseCheck(_))

object SseCheckMaterializer {
  val BodyString: CheckMaterializer[BodyStringCheckType, SseCheck, String, String] =
    new SseCheckMaterializer[BodyStringCheckType, String](identityPreparer)

  def jmesPath(jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, SseCheck, String, JsonNode] =
    new SseCheckMaterializer[JmesPathCheckType, JsonNode](jsonParsers.safeParse)

  def jsonPath(jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, SseCheck, String, JsonNode] =
    new SseCheckMaterializer[JsonPathCheckType, JsonNode](jsonParsers.safeParse)

  val Regex: CheckMaterializer[RegexCheckType, SseCheck, String, String] =
    new SseCheckMaterializer[RegexCheckType, String](identityPreparer)

  val Substring: CheckMaterializer[SubstringCheckType, SseCheck, String, String] =
    new SseCheckMaterializer[SubstringCheckType, String](identityPreparer)
}
