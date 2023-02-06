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

package io.gatling.http.check.ws

import io.gatling.commons.validation._
import io.gatling.core.check.{ identityPreparer, CheckMaterializer, Preparer }
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.json.JsonParsers

import com.fasterxml.jackson.databind.JsonNode

object WsCheckMaterializer {
  final class Binary[T, P](override val preparer: Preparer[Array[Byte], P]) extends CheckMaterializer[T, WsCheck.Binary, Array[Byte], P](new WsCheck.Binary(_))

  object Binary {
    val BodyBytes: CheckMaterializer[BodyBytesCheckType, WsCheck.Binary, Array[Byte], Array[Byte]] =
      new Binary[BodyBytesCheckType, Array[Byte]](identityPreparer)

    val BodyLength: CheckMaterializer[BodyBytesCheckType, WsCheck.Binary, Array[Byte], Int] =
      new Binary[BodyBytesCheckType, Int](_.length.success)
  }

  final class Text[T, P](override val preparer: Preparer[String, P]) extends CheckMaterializer[T, WsCheck.Text, String, P](new WsCheck.Text(_))

  object Text {
    val BodyString: CheckMaterializer[BodyStringCheckType, WsCheck.Text, String, String] =
      new Text[BodyStringCheckType, String](identityPreparer)

    def jmesPath(jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, WsCheck.Text, String, JsonNode] =
      new Text[JmesPathCheckType, JsonNode](jsonParsers.safeParse)

    def jsonPath(jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, WsCheck.Text, String, JsonNode] =
      new Text[JsonPathCheckType, JsonNode](jsonParsers.safeParse)

    val Regex: CheckMaterializer[RegexCheckType, WsCheck.Text, String, String] =
      new Text[RegexCheckType, String](identityPreparer)

    val Substring: CheckMaterializer[SubstringCheckType, WsCheck.Text, String, String] =
      new Text[SubstringCheckType, String](identityPreparer)
  }
}
