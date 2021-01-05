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

package io.gatling.http.action.sse.fsm

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.core.action.Action
import io.gatling.core.session.Session

import com.typesafe.scalalogging.StrictLogging

class SseClosingState(fsm: SseFsm, actionName: String, session: Session, next: Action, timestamp: Long) extends SseState(fsm) with StrictLogging {

  import fsm._

  override def onSseStreamClosed(closeEnd: Long): NextSseState = {
    logger.debug("Stream closed")
    val newSession = logResponse(session, actionName, timestamp, closeEnd, OK, None, None).remove(sseName)
    NextSseState(new SseClosedState(fsm), () => next ! newSession)
  }

  override def onSseStreamCrashed(t: Throwable, closeStart: Long): NextSseState = {
    logger.debug("SSE stream crashed while waiting for socket close")
    // crash, close anyway
    val newSession = logResponse(session, actionName, closeStart, timestamp, KO, None, Some(t.getMessage)).markAsFailed.remove(sseName)
    NextSseState(new SseClosedState(fsm), () => next ! newSession)
  }
}
