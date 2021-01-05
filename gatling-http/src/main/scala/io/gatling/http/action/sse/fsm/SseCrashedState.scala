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

import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.sse.SseMessageCheckSequence

import com.typesafe.scalalogging.StrictLogging

class SseCrashedState(fsm: SseFsm, errorMessage: String) extends SseState(null) with StrictLogging {

  override def onClientCloseRequest(actionName: String, session: Session, next: Action): NextSseState = {
    fsm.statsEngine.logCrash(session.scenario, session.groups, actionName, s"Client issued close order but SSE stream was already crashed: $errorMessage")
    val newSession = session.markAsFailed.remove(fsm.sseName)
    NextSseState(new SseClosedState(fsm), () => next ! newSession)
  }

  override def onSetCheck(actionName: String, checkSequences: List[SseMessageCheckSequence], session: Session, next: Action): NextSseState = {
    logger.debug(s"Client set checks but SSE stream was already crashed: $errorMessage")
    fsm.statsEngine.logCrash(session.scenario, session.groups, actionName, errorMessage)
    NextSseState(this, () => next ! session.markAsFailed)
  }
}
