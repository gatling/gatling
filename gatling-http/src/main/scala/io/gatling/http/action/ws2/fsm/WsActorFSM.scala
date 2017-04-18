/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.ws2.fsm

import io.gatling.core.action.Action
import io.gatling.core.akka.BaseActor
import io.gatling.core.session.Session
import io.gatling.http.action.ws2.{ WsCheck, WsCheckSequence }

import akka.actor.FSM
import org.asynchttpclient.ws.WebSocket

sealed trait WsActorState
case object Init extends WsActorState
case object Connecting extends WsActorState
case object PerformingCheck extends WsActorState
case object Idle extends WsActorState
case object Closing extends WsActorState
case object Crashed extends WsActorState

sealed trait WsActorData
case object InitData extends WsActorData
case class ConnectingData(session: Session, next: Either[Action, SendTextMessage], timestamp: Long, remainingTries: Int) extends WsActorData
case class PerformingCheckData(
  webSocket:               WebSocket,
  currentCheck:            WsCheck,
  remainingChecks:         List[WsCheck],
  checkSequenceStart:      Long,
  checkSequenceTimeoutId:  Long,
  remainingCheckSequences: List[WsCheckSequence],
  session:                 Session,
  next:                    Either[Action, SendTextMessage]
) extends WsActorData
case class IdleData(session: Session, webSocket: WebSocket) extends WsActorData
case class ClosingData(actionName: String, session: Session, next: Action, timestamp: Long) extends WsActorData
case class CrashedData(errorMessage: Option[String]) extends WsActorData

class WsActorFSM extends BaseActor with FSM[WsActorState, WsActorData]
