/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.sse.SseMessageCheckSequence

class SseCrashedState(fsm: SseFsm, errorMessage: Option[String]) extends SseState(fsm) with StrictLogging {

  import fsm._

  override def onClientCloseRequest(actionName: String, session: Session, next: Action): NextSseState = {
    val newSession = errorMessage match {
      case Some(mess) =>
        val newSession = session.markAsFailed
        statsEngine.logCrash(newSession, actionName, s"Client issued close order but SSE stream was already crashed: $mess")
        newSession
      case _ =>
        logger.info("Client issued close order but SSE stream was already closed")
        session
    }

    NextSseState(
      new SseClosedState(fsm),
      () => next ! newSession.remove(wsName)
    )
  }

  override def onSetCheck(actionName: String, checkSequences: List[SseMessageCheckSequence], session: Session, next: Action): NextSseState = {
    // FIXME sent message so be stashed until reconnect, instead of failed
    val loggedMessage = errorMessage match {
      case Some(mess) => s"Client issued message but SSE stream was already crashed: $mess"
      case _          => "Client issued message but SSE stream was already closed"
    }

    logger.info(loggedMessage)

    // perform blocking reconnect
    SseConnectingState.gotoConnecting(fsm, session, Right(SetCheck(actionName, checkSequences, session, next)))
  }
}
