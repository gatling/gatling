/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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
import io.gatling.http.action.sse.fsm.ServerSentEvent

import com.fasterxml.jackson.databind.JsonNode

trait SseCheckSupport {
  implicit def checkBuilder2SseCheck[T, P](checkBuilder: CheckBuilder[T, P])(implicit
      materializer: CheckMaterializer[T, SseCheck, ServerSentEvent, P]
  ): SseCheck =
    checkBuilder.build(materializer)

  implicit def validate2SseCheck[T, P, X](
      validate: CheckBuilder.Validate[T, P, X]
  )(implicit materializer: CheckMaterializer[T, SseCheck, ServerSentEvent, P]): SseCheck =
    validate.exists

  implicit def find2SseCheck[T, P, X](
      find: CheckBuilder.Find[T, P, X]
  )(implicit materializer: CheckMaterializer[T, SseCheck, ServerSentEvent, P]): SseCheck =
    find.find.exists

  implicit def sseJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, SseCheck, ServerSentEvent, JsonNode] =
    SseCheckMaterializer.jsonPath(jsonParsers)

  implicit def sseJmesPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, SseCheck, ServerSentEvent, JsonNode] =
    SseCheckMaterializer.jmesPath(jsonParsers)

  implicit val sseRegexCheckMaterializer: CheckMaterializer[RegexCheckType, SseCheck, ServerSentEvent, String] = SseCheckMaterializer.Regex

  implicit val sseSubstringCheckMaterializer: CheckMaterializer[SubstringCheckType, SseCheck, ServerSentEvent, String] =
    SseCheckMaterializer.Substring

  implicit val sseBodyStringCheckMaterializer: CheckMaterializer[BodyStringCheckType, SseCheck, ServerSentEvent, String] =
    SseCheckMaterializer.BodyString

  val sseEvent: CheckBuilder.Find[SseEventCheckType, ServerSentEvent, String] = SseEventCheckBuilder
  val sseData: CheckBuilder.Find[SseDataCheckType, ServerSentEvent, String] = SseDataCheckBuilder
  val sseId: CheckBuilder.Find[SseIdCheckType, ServerSentEvent, String] = SseIdCheckBuilder
  val sseRetry: CheckBuilder.Find[SseRetryCheckType, ServerSentEvent, Int] = SseRetryCheckBuilder

  implicit val sseEventCheckMaterializer: CheckMaterializer[SseEventCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    SseCheckMaterializer.Event
  implicit val sseDataCheckMaterializer: CheckMaterializer[SseDataCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    SseCheckMaterializer.Data
  implicit val sseIdCheckMaterializer: CheckMaterializer[SseIdCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    SseCheckMaterializer.Id
  implicit val sseRetryCheckMaterializer: CheckMaterializer[SseRetryCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    SseCheckMaterializer.Retry

  implicit val sseUntypedCheckIfMaker: UntypedCheckIfMaker[SseCheck] = _.checkIf(_)

  implicit val sseTypedCheckIfMaker: TypedCheckIfMaker[ServerSentEvent, SseCheck] = _.checkIf(_)
}
