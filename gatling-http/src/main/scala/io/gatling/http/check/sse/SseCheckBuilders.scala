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
import io.gatling.core.check.{ CheckBuilder, FindExtractor }
import io.gatling.core.session._
import io.gatling.http.action.sse.fsm.ServerSentEvent

sealed trait SseEventCheckType
object SseEventCheckBuilder
    extends CheckBuilder.Find.Default[SseEventCheckType, ServerSentEvent, String](
      extractor = new FindExtractor[ServerSentEvent, String]("sseEvent", _.event.success).expressionSuccess,
      logActualValueInError = true
    )

sealed trait SseDataCheckType
object SseDataCheckBuilder
    extends CheckBuilder.Find.Default[SseDataCheckType, ServerSentEvent, String](
      extractor = new FindExtractor[ServerSentEvent, String]("sseData", _.data.success).expressionSuccess,
      logActualValueInError = true
    )

sealed trait SseIdCheckType
object SseIdCheckBuilder
    extends CheckBuilder.Find.Default[SseIdCheckType, ServerSentEvent, String](
      extractor = new FindExtractor[ServerSentEvent, String]("sseId", _.id.success).expressionSuccess,
      logActualValueInError = true
    )

sealed trait SseRetryCheckType
object SseRetryCheckBuilder
    extends CheckBuilder.Find.Default[SseRetryCheckType, ServerSentEvent, Int](
      extractor = new FindExtractor[ServerSentEvent, Int]("sseRetry", _.retry.success).expressionSuccess,
      logActualValueInError = true
    )
