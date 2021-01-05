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
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.json.JsonParsers

import com.fasterxml.jackson.databind.JsonNode

trait WsCheckSupport {

  implicit def checkBuilder2WsTextCheck[T, P, X](
      checkBuilder: CheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsTextCheck, String, P]): WsTextCheck =
    checkBuilder.build(materializer)

  implicit def validatorCheckBuilder2WsTextCheck[T, P, X](
      validatorCheckBuilder: ValidatorCheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsTextCheck, String, P]): WsTextCheck =
    validatorCheckBuilder.exists

  implicit def findCheckBuilder2WsTextCheck[T, P, X](
      findCheckBuilder: FindCheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsTextCheck, String, P]): WsTextCheck =
    findCheckBuilder.find.exists

  implicit def checkBuilder2WsBinaryCheck[T, P, X](
      checkBuilder: CheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsBinaryCheck, Array[Byte], P]): WsBinaryCheck =
    checkBuilder.build(materializer)

  implicit def validatorCheckBuilder2WsBinaryCheck[T, P, X](
      validatorCheckBuilder: ValidatorCheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsBinaryCheck, Array[Byte], P]): WsBinaryCheck =
    validatorCheckBuilder.exists

  implicit def findCheckBuilder2WsBinaryCheck[T, P, X](
      findCheckBuilder: FindCheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, WsBinaryCheck, Array[Byte], P]): WsBinaryCheck =
    findCheckBuilder.find.exists

  implicit def wsJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, WsTextCheck, String, JsonNode] =
    WsTextCheckMaterializer.jsonPath(jsonParsers)

  implicit def wsJmesPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, WsTextCheck, String, JsonNode] =
    WsTextCheckMaterializer.jmesPath(jsonParsers)

  implicit val wsRegexCheckMaterializer: CheckMaterializer[RegexCheckType, WsTextCheck, String, String] = WsTextCheckMaterializer.Regex

  implicit val wsBodyStringCheckMaterializer: CheckMaterializer[BodyStringCheckType, WsTextCheck, String, String] =
    WsTextCheckMaterializer.BodyString

  implicit val wsSubstringCheckMaterializer: CheckMaterializer[SubstringCheckType, WsTextCheck, String, String] =
    WsTextCheckMaterializer.Substring

  implicit val wsBodyBytesCheckMaterializer: CheckMaterializer[BodyBytesCheckType, WsBinaryCheck, Array[Byte], Array[Byte]] =
    WsBinaryCheckMaterializer.BodyBytes

  implicit val wsBodyLengthCheckMaterializer: CheckMaterializer[BodyBytesCheckType, WsBinaryCheck, Array[Byte], Int] =
    WsBinaryCheckMaterializer.BodyLength
}
