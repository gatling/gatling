/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

trait WhenClosing { this: SseActor =>

  when(Closing) {
    case Event(_: SseReceived, ClosingData(_, session, _, _)) =>
      logUnmatchedServerMessage(session)
      stay()

    case Event(SseStreamClosed(timestamp), ClosingData(actionName, session, next, closeStart)) =>
      // server has acked closing
      logger.info("Socket closed")
      val newSession = logResponse(session, actionName, closeStart, timestamp, OK, None, None)
      next ! newSession.remove(wsName)
      stop()

    case Event(SseStreamCrashed(t, timestamp), ClosingData(actionName, session, next, closeStart)) =>
      logger.info("SSE stream crashed while waiting for socket close")
      // crash, close anyway
      val newSession = logResponse(session, actionName, closeStart, timestamp, KO, None, Some(t.getMessage))
      next ! newSession.markAsFailed.remove(wsName)
      stop()
  }
}
