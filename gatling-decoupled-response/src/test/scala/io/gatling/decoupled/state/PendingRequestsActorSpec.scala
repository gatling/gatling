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

package io.gatling.decoupled.state

import java.time.Instant
import java.util.UUID
import io.gatling.AkkaSpec
import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.decoupled.models.{ ExecutionId, ExecutionPhase, MissingPhase, TriggerPhase }
import io.gatling.decoupled.state.PendingRequestsActor.{ DecoupledResponseReceived, MessageAck, RequestTriggered, WaitResponseTimeout }
import io.netty.channel.EventLoop
import org.scalatest.concurrent.Eventually
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

import scala.concurrent.duration.{ FiniteDuration, _ }
import scala.language.postfixOps

class PendingRequestsActorSpec extends AkkaSpec with Eventually {

  behavior of "PendingRequestsActor"

  it should "log in StatsEngine time distance between triggering and first execution phase" in new Fixtures {
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    actor ! DecoupledResponseReceived(executionId, phases)

    verifyPhasesLogs(Seq(triggerPhase, phases.head))
  }

  it should "log in StatsEngine time distance before two consecutive execution phases" in new Fixtures {
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    actor ! DecoupledResponseReceived(executionId, phases)

    verifyPhasesLogs(phases)
  }
  it should "manage case when response is received before trigger" in new Fixtures {
    actor ! DecoupledResponseReceived(executionId, phases)
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)

    verifyPhasesLogs(triggerPhase +: phases)
  }

  it should "trigger next action once response is received" in new Fixtures {
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    actor ! DecoupledResponseReceived(executionId, phases)

    verifyNextActionTriggered
  }

  it should "error if no phases in response" in new Fixtures {
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    actor ! DecoupledResponseReceived(executionId, Seq.empty)

    verifyStatsEngineErrorCall
    verifyNextActionTriggered
  }

  it should "error if there are duplicated phase names" in new Fixtures {
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    actor ! DecoupledResponseReceived(executionId, triggerPhase +: phases)

    verifyStatsEngineErrorCall
    verifyNextActionTriggered
  }

  it should "error if times are reversed" in new Fixtures {
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    actor ! DecoupledResponseReceived(executionId, phases.reverse)

    verifyStatsEngineErrorCall
    verifyNextActionTriggered
  }

  it should "error all steps in case of timeout on phases" in new Fixtures {
    actor ! RequestTriggered(otherExecutionId, triggerPhase, otherSession, otherNextAction)
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)

    actor ! DecoupledResponseReceived(otherExecutionId, phases)

    verifyPhasesLogs(triggerPhase +: timeoutPhases)
    verifyNextActionTriggered
  }

  it should "error all steps in case of timeout on phases (even if timeout is received before phases are known)" in new Fixtures {
    actor ! RequestTriggered(otherExecutionId, triggerPhase, otherSession, otherNextAction)
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)

    actor ! WaitResponseTimeout(executionId)

    actor ! DecoupledResponseReceived(otherExecutionId, phases)

    verifyPhasesLogs(triggerPhase +: timeoutPhases)
    verifyNextActionTriggered
  }

  it should "error if trigger is received twice" in new Fixtures {
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    actor ! DecoupledResponseReceived(executionId, phases)

    verifyStatsEngineErrorCall
    verifyNextActionTriggered
  }

  it should "acknowledge messages" in new Fixtures {
    actor ! RequestTriggered(executionId, triggerPhase, session, nextAction)
    expectMsg(MessageAck)

    actor ! DecoupledResponseReceived(executionId, phases)
    expectMsg(MessageAck)
  }

  sealed trait Fixtures {

    val statsEngine = mock[StatsEngine]

    val session = Session(
      "PendingRequestsActorSpec",
      0,
      Map.empty,
      OK,
      List.empty,
      _ => (),
      mock[EventLoop]
    )
    val otherSession = session.copy(userId = 1)

    val nextAction = mock[Action]
    val otherNextAction = mock[Action]

    val executionId = ExecutionId(UUID.randomUUID().toString)
    val otherExecutionId = ExecutionId(UUID.randomUUID().toString)

    val initialTime = Instant.now
    val brokenClock = new Clock {
      override def nowMillis: Long = initialTime.toEpochMilli + 1
    }

    val pendingTimeout: FiniteDuration = 50 millis

    val actor = PendingRequestsActor(system, statsEngine, brokenClock, pendingTimeout)

    val triggerPhase = TriggerPhase(initialTime)
    val phases = (1 to 5).map { i =>
      ExecutionPhase(s"phase-$i", initialTime.plusMillis(i * 1000))
    }.toList
    val timeoutPhases: Seq[ExecutionPhase] = phases.map { phase =>
      MissingPhase(phase.name, Instant.ofEpochMilli(brokenClock.nowMillis))
    }

    def verifyPhasesLogs(phases: Seq[ExecutionPhase]): Unit =
      phases.sliding(2).foreach { case pair =>
        verifyStatsEngineCall(pair.head, pair.tail.head)
      }

    def verifyStatsEngineCall(from: ExecutionPhase, to: ExecutionPhase): Unit = {

      val state = to match {
        case _: MissingPhase => KO
        case _               => OK
      }

      eventually {
        verify(statsEngine, times(1)).logResponse(
          session.scenario,
          session.groups,
          PendingRequestsActor.genName(executionId, from.name, to.name),
          from.time.toEpochMilli,
          to.time.toEpochMilli,
          state,
          None,
          None
        )
      }
    }

    def verifyStatsEngineErrorCall: Unit =
      eventually {
        verify(statsEngine, times(1)).logResponse(
          session.scenario,
          session.groups,
          PendingRequestsActor.genErrorName(executionId),
          initialTime.toEpochMilli,
          brokenClock.nowMillis,
          KO,
          None,
          None
        )
      }

    def verifyNextActionNotTriggered: Unit =
      verify(nextAction, never) ! any[Session]

    def verifyNextActionTriggered: Unit =
      eventually {
        verify(nextAction, times(1)) ! any[Session]
      }

  }

}
