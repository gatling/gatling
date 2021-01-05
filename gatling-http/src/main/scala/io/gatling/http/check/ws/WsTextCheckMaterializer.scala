/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.json.JsonParsers

import com.fasterxml.jackson.databind.JsonNode

class WsTextCheckMaterializer[T, P](override val preparer: Preparer[String, P]) extends CheckMaterializer[T, WsTextCheck, String, P](new WsTextCheck(_))

object WsTextCheckMaterializer {

  val BodyString: CheckMaterializer[BodyStringCheckType, WsTextCheck, String, String] =
    new WsTextCheckMaterializer[BodyStringCheckType, String](identityPreparer)

  def jmesPath(jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, WsTextCheck, String, JsonNode] =
    new WsTextCheckMaterializer[JmesPathCheckType, JsonNode](jsonParsers.safeParse)

  def jsonPath(jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, WsTextCheck, String, JsonNode] =
    new WsTextCheckMaterializer[JsonPathCheckType, JsonNode](jsonParsers.safeParse)

  val Regex: CheckMaterializer[RegexCheckType, WsTextCheck, String, String] =
    new WsTextCheckMaterializer[RegexCheckType, String](identityPreparer)

  val Substring: CheckMaterializer[SubstringCheckType, WsTextCheck, String, String] =
    new WsTextCheckMaterializer[SubstringCheckType, String](identityPreparer)
}
