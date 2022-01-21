/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.json.JsonParsers

import com.fasterxml.jackson.databind.JsonNode

trait WsCheckSupport {

  implicit def checkBuilder2WsTextCheck[T, P](
      checkBuilder: CheckBuilder[T, P]
  )(implicit materializer: CheckMaterializer[T, WsCheck.Text, String, P]): WsCheck.Text =
    checkBuilder.build(materializer)

  implicit def validate2WsTextCheck[T, P, X](
      validate: CheckBuilder.Validate[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsCheck.Text, String, P]): WsCheck.Text =
    validate.exists

  implicit def find2WsTextCheck[T, P, X](
      find: CheckBuilder.Find[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsCheck.Text, String, P]): WsCheck.Text =
    find.find.exists

  implicit def checkBuilder2WsBinaryCheck[T, P](
      checkBuilder: CheckBuilder[T, P]
  )(implicit materializer: CheckMaterializer[T, WsCheck.Binary, Array[Byte], P]): WsCheck.Binary =
    checkBuilder.build(materializer)

  implicit def validate2WsBinaryCheck[T, P, X](
      validate: CheckBuilder.Validate[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsCheck.Binary, Array[Byte], P]): WsCheck.Binary =
    validate.exists

  implicit def find2WsBinaryCheck[T, P, X](
      find: CheckBuilder.Find[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsCheck.Binary, Array[Byte], P]): WsCheck.Binary =
    find.find.exists

  implicit def wsJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, WsCheck.Text, String, JsonNode] =
    WsCheckMaterializer.Text.jsonPath(jsonParsers)

  implicit def wsJmesPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, WsCheck.Text, String, JsonNode] =
    WsCheckMaterializer.Text.jmesPath(jsonParsers)

  implicit val wsRegexCheckMaterializer: CheckMaterializer[RegexCheckType, WsCheck.Text, String, String] = WsCheckMaterializer.Text.Regex

  implicit val wsBodyStringCheckMaterializer: CheckMaterializer[BodyStringCheckType, WsCheck.Text, String, String] =
    WsCheckMaterializer.Text.BodyString

  implicit val wsSubstringCheckMaterializer: CheckMaterializer[SubstringCheckType, WsCheck.Text, String, String] =
    WsCheckMaterializer.Text.Substring

  implicit val wsBodyBytesCheckMaterializer: CheckMaterializer[BodyBytesCheckType, WsCheck.Binary, Array[Byte], Array[Byte]] =
    WsCheckMaterializer.Binary.BodyBytes

  implicit val wsBodyLengthCheckMaterializer: CheckMaterializer[BodyBytesCheckType, WsCheck.Binary, Array[Byte], Int] =
    WsCheckMaterializer.Binary.BodyLength

  implicit val wsTextUntypedCheckIfMaker: UntypedCheckIfMaker[WsCheck.Text] = _.checkIf(_)

  implicit val wsTextTypedCheckIfMaker: TypedCheckIfMaker[String, WsCheck.Text] = _.checkIf(_)

  implicit val wsBinaryUntypedCheckIfMaker: UntypedCheckIfMaker[WsCheck.Binary] = _.checkIf(_)

  implicit val wsBinaryTypedCheckIfMaker: TypedCheckIfMaker[Array[Byte], WsCheck.Binary] = _.checkIf(_)
}
