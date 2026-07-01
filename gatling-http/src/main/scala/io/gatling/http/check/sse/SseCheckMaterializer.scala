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

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.json.JsonParsers
import io.gatling.http.action.sse.fsm.ServerSentEvent

import com.fasterxml.jackson.databind.JsonNode

final class SseCheckMaterializer[T, P](override val preparer: Preparer[ServerSentEvent, P])
    extends CheckMaterializer[T, SseCheck, ServerSentEvent, P](new SseCheck(_))

object SseCheckMaterializer {
  private val DataStringPreparer: Preparer[ServerSentEvent, String] =
    event => event.data.getOrElse("").success

  val BodyString: CheckMaterializer[BodyStringCheckType, SseCheck, ServerSentEvent, String] =
    new SseCheckMaterializer[BodyStringCheckType, String](DataStringPreparer)

  def jmesPath(jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, SseCheck, ServerSentEvent, JsonNode] =
    new SseCheckMaterializer[JmesPathCheckType, JsonNode](event =>
      event.data match {
        case Some(data) => jsonParsers.safeParse(data)
        case None       => "No SSE data field".failure
      }
    )

  def jsonPath(jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, SseCheck, ServerSentEvent, JsonNode] =
    new SseCheckMaterializer[JsonPathCheckType, JsonNode](event =>
      event.data match {
        case Some(data) => jsonParsers.safeParse(data)
        case None       => "No SSE data field".failure
      }
    )

  val Regex: CheckMaterializer[RegexCheckType, SseCheck, ServerSentEvent, String] =
    new SseCheckMaterializer[RegexCheckType, String](DataStringPreparer)

  val Substring: CheckMaterializer[SubstringCheckType, SseCheck, ServerSentEvent, String] =
    new SseCheckMaterializer[SubstringCheckType, String](DataStringPreparer)

  val Event: CheckMaterializer[SseEventCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    new SseCheckMaterializer[SseEventCheckType, ServerSentEvent](identityPreparer)

  val Data: CheckMaterializer[SseDataCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    new SseCheckMaterializer[SseDataCheckType, ServerSentEvent](identityPreparer)

  val Id: CheckMaterializer[SseIdCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    new SseCheckMaterializer[SseIdCheckType, ServerSentEvent](identityPreparer)

  val Retry: CheckMaterializer[SseRetryCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    new SseCheckMaterializer[SseRetryCheckType, ServerSentEvent](identityPreparer)
}
