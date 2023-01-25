/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.decoupled.state

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.decoupled.models.{ ExecutionPhase, MissingPhase }
import io.gatling.decoupled.models.ExecutionId.ExecutionId

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorRefFactory, Props, Timers }

object PendingRequestsActor {

  private[state] def apply(actorRefFactory: ActorRefFactory, statsEngine: StatsEngine, clock: Clock, pendingTimeout: FiniteDuration): ActorRef =
    actorRefFactory.actorOf(
      Props(new PendingRequestsActor(statsEngine, clock, pendingTimeout))
    )

  final case class ActorState(
      waitingResponse: Map[ExecutionId, RequestTriggered],
      waitingTrigger: Map[ExecutionId, DecoupledResponseReceived],
      expectedPhases: Seq[String]
  ) {
    def withPendingResponse(response: DecoupledResponseReceived): ActorState =
      copy(waitingTrigger = waitingTrigger + (response.id -> response))

    def withPendingTrigger(trigger: RequestTriggered): ActorState =
      copy(waitingResponse = waitingResponse + (trigger.id -> trigger))

    def filterManagedId(id: ExecutionId): ActorState =
      copy(waitingResponse = waitingResponse - id, waitingTrigger = waitingTrigger - id)

    def withExpectedPhases(names: Seq[String]): ActorState =
      copy(expectedPhases = names)

  }

  object ActorState {
    val empty: ActorState = ActorState(Map.empty, Map.empty, Seq.empty)
  }

  sealed trait ActorMessage
  final case class RequestTriggered(id: ExecutionId, initialPhase: ExecutionPhase, session: Session, next: Action) extends ActorMessage {
    def triggerNextAction: Unit =
      next ! session
  }
  final case class DecoupledResponseReceived(id: ExecutionId, executionPhases: Seq[ExecutionPhase]) extends ActorMessage
  final case class WaitResponseTimeout(id: ExecutionId) extends ActorMessage
  final case class WaitTriggerTimeout(id: ExecutionId) extends ActorMessage

  sealed trait ActorResponse
  final case object MessageAck extends ActorResponse

  def genName(id: ExecutionId, from: String, to: String): String =
    s"$from -> $to"
  val errorName = "Error"
  def genErrorName(id: ExecutionId): String =
    s"$id: $errorName"
}

private[state] class PendingRequestsActor(statsEngine: StatsEngine, clock: Clock, pendingTimeout: FiniteDuration) extends Actor with ActorLogging with Timers {
  import PendingRequestsActor._

  override def receive: Receive = receiveWithState(ActorState.empty)

  private def receiveWithState(state: ActorState): Receive = {

    case trigger @ RequestTriggered(id, _, _, _) if state.waitingResponse.contains(id) =>
      log.error("Duplicate trigger received: {}", trigger)
      onWrongMessageReceived(id, state)
      ackMessage

    case trigger @ RequestTriggered(id, _, _, _) if state.waitingTrigger.contains(id) =>
      val response = state.waitingTrigger(id)
      onTriggerAndResponseAvailable(trigger, response, state)
      ackMessage

    case trigger: RequestTriggered =>
      onOnlyTriggerAvailable(trigger, state)
      ackMessage

    case response @ DecoupledResponseReceived(_, phases) if phases.isEmpty =>
      log.error("Response with no phases received: {}", response)
      onWrongMessageReceived(response.id, state)
      ackMessage

    case response @ DecoupledResponseReceived(_, phases) if hasDuplicatedPhaseNames(phases) =>
      log.error("Response with duplicate phase names received: {}", response)
      onWrongMessageReceived(response.id, state)
      ackMessage

    case response @ DecoupledResponseReceived(id, _) if state.waitingResponse.contains(id) =>
      val trigger = state.waitingResponse(id)
      onTriggerAndResponseAvailable(trigger, response, state)
      ackMessage

    case response: DecoupledResponseReceived =>
      onOnlyResponseAvailable(response, state)
      ackMessage

    case WaitResponseTimeout(id) if state.expectedPhases.nonEmpty && state.waitingResponse.contains(id) =>
      log.error("Response not received: {}", id)
      onPhasesTimeout(state.waitingResponse(id), state)
      ackMessage

    case WaitResponseTimeout(id) =>
      startResponseTimeout(id)

    case WaitTriggerTimeout(id) =>
      log.error("Trigger not received: {}", id)
      onWrongMessageReceived(id, state)
      ackMessage

  }

  private def onOnlyTriggerAvailable(trigger: RequestTriggered, state: ActorState): Unit = {
    startResponseTimeout(trigger.id)

    context.become(
      receiveWithState(state.withPendingTrigger(trigger)),
      true
    )
  }

  private def startResponseTimeout(id: ExecutionId) =
    timers.startSingleTimer(id, WaitResponseTimeout(id), pendingTimeout)

  private def onOnlyResponseAvailable(response: DecoupledResponseReceived, state: ActorState): Unit = {
    timers.startSingleTimer(response.id, WaitTriggerTimeout(response.id), pendingTimeout)

    context.become(
      receiveWithState(state.withPendingResponse(response)),
      true
    )
  }

  private def hasDuplicatedPhaseNames(phases: Seq[ExecutionPhase]): Boolean = {
    val allNames = phases.map(_.name) :+ ExecutionPhase.triggerPhaseName
    allNames.diff(allNames.distinct).nonEmpty
  }

  private def onTriggerAndResponseAvailable(trigger: RequestTriggered, response: DecoupledResponseReceived, state: ActorState) = {
    timers.cancel(trigger.id)

    val allPhases = (trigger.initialPhase +: response.executionPhases).toList
    if (phaseTimesAreSequential(allPhases)) {
      onValidTriggerAndResponseAvailable(trigger, allPhases, state)

    } else {
      log.error("Non sequential times received: {}", allPhases)
      onWrongMessageReceived(response.id, state)
    }

  }

  private def phaseTimesAreSequential(phases: List[ExecutionPhase]) =
    phases.sliding(2).forall {
      case from :: to :: Nil => from.time.isBefore(to.time)
      case _                 => false
    }

  private def onValidTriggerAndResponseAvailable(trigger: RequestTriggered, allPhases: List[ExecutionPhase], state: ActorState) = {
    logAllPhases(trigger.id, trigger.session, allPhases)

    trigger.triggerNextAction

    context.become(
      receiveWithState(
        state
          .filterManagedId(trigger.id)
          .withExpectedPhases(allPhases.map(_.name))
      ),
      true
    )

  }

  private def onWrongMessageReceived(id: ExecutionId, state: ActorState) = {
    state.waitingResponse.get(id).foreach { trigger =>
      logFailure(id, trigger.session, trigger.initialPhase)

      trigger.triggerNextAction
    }

    context.become(
      receiveWithState(state.filterManagedId(id)),
      true
    )
  }

  private def onPhasesTimeout(trigger: RequestTriggered, state: ActorState) = {

    val otherPhases = state.waitingTrigger.get(trigger.id).toList.flatMap(_.executionPhases)

    val availablePhases = trigger.initialPhase +: otherPhases

    val filledPhases = fillMissingPhases(availablePhases, state.expectedPhases, Instant.ofEpochMilli(clock.nowMillis))

    logAllPhases(trigger.id, trigger.session, filledPhases)

    trigger.triggerNextAction

    context.become(
      receiveWithState(state.filterManagedId(trigger.id)),
      true
    )
  }

  private def fillMissingPhases(phases: Seq[ExecutionPhase], expectedPhases: Seq[String], timeoutTime: Instant) = {

    val availablePhases = phases.map(phase => (phase.name, phase)).toMap

    expectedPhases.map { phaseName =>
      availablePhases.getOrElse(phaseName, MissingPhase(phaseName, timeoutTime))
    }

  }

  @SuppressWarnings(Array("org.wartremover.warts.ListUnapply"))
  private def logAllPhases(id: ExecutionId, session: Session, allPhases: Seq[ExecutionPhase]) =
    allPhases.sliding(2).foreach {
      case from :: to :: Nil =>
        logPhaseTransition(id, session, from, to)
      case _ => ()
    }

  private def logPhaseTransition(
      id: ExecutionId,
      session: Session,
      from: ExecutionPhase,
      to: ExecutionPhase
  ) = {

    val state = to match {
      case _: MissingPhase => KO
      case _               => OK
    }

    statsEngine.logResponse(
      session.scenario,
      session.groups,
      genName(id, from.name, to.name),
      from.time.toEpochMilli,
      to.time.toEpochMilli,
      state,
      None,
      None
    )
  }

  private def logFailure(id: ExecutionId, session: Session, from: ExecutionPhase) =
    statsEngine.logResponse(
      session.scenario,
      session.groups,
      genErrorName(id),
      from.time.toEpochMilli,
      clock.nowMillis,
      KO,
      None,
      None
    )

  private def ackMessage =
    sender() ! MessageAck

}
