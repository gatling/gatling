/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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
import io.gatling.core.akka.BaseActor
import io.gatling.core.session.Session
import io.gatling.http.check.sse.{ SseMessageCheck, SseMessageCheckSequence }

import akka.actor.FSM

sealed trait SseActorState
case object Init extends SseActorState
case object Connecting extends SseActorState
case object PerformingCheck extends SseActorState
case object Idle extends SseActorState
case object Closing extends SseActorState
case object Crashed extends SseActorState

sealed trait SseActorData
case object InitData extends SseActorData
final case class ConnectingData(session: Session, next: Either[Action, SetCheck], timestamp: Long, remainingTries: Int) extends SseActorData
final case class PerformingCheckData(
    stream:                  SseStream,
    currentCheck:            SseMessageCheck,
    remainingChecks:         List[SseMessageCheck],
    checkSequenceStart:      Long,
    checkSequenceTimeoutId:  Long,
    remainingCheckSequences: List[SseMessageCheckSequence],
    session:                 Session,
    next:                    Either[Action, SetCheck]
) extends SseActorData
final case class IdleData(session: Session, stream: SseStream) extends SseActorData
final case class ClosingData(actionName: String, session: Session, next: Action, timestamp: Long) extends SseActorData
final case class CrashedData(errorMessage: Option[String]) extends SseActorData

class SseActorFSM extends BaseActor with FSM[SseActorState, SseActorData]
