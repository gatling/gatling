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

package io.gatling.decoupled.action

import java.time.Instant
import java.util.UUID

import akka.Done
import akka.actor.ActorRef
import org.mockito.Mockito._
import io.gatling.commons.util.Clock
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.pause.Constant
import io.gatling.core.protocol.ProtocolComponentsRegistries
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.decoupled.models.{ ExecutionId, TriggerPhase }
import io.gatling.decoupled.state.PendingRequestsState
import io.gatling.AkkaSpec
import io.gatling.core.session.Expression
import io.gatling.core.ValidationImplicits
import io.netty.channel.EventLoopGroup
import io.gatling.core.EmptySession
import io.gatling.decoupled.models.ExecutionId.ExecutionId

import scala.concurrent.Future

class WaitDecoupledResponseActionSpec extends AkkaSpec with EmptySession {

  "WaitDecoupledResponseAction" should "notify state there is a response to be waited" in new Fixtures {

    val expectedTriggerPhase = TriggerPhase(Instant.ofEpochMilli(nowMilli))
    when(state.registerTrigger(id, expectedTriggerPhase, session, nextAction)).thenReturn(Future.successful(Done))

    val action = new WaitDecoupledResponseAction("test", state, nextAction, idExpression, scenario)
    action.sendRequest(session)

    verify(state, times(1)).registerTrigger(id, expectedTriggerPhase, session, nextAction)
  }

  "WaitDecoupledResponseAction" should "not trigger next action as state will do that when response is received" in new Fixtures {
    val expectedTriggerPhase = TriggerPhase(Instant.ofEpochMilli(nowMilli))
    when(state.registerTrigger(id, expectedTriggerPhase, session, nextAction)).thenReturn(Future.successful(Done))

    val action = new WaitDecoupledResponseAction("test", state, nextAction, idExpression, scenario)
    action.sendRequest(session)

    verify(nextAction, never()).!(session)
  }

  trait Fixtures extends ValidationImplicits {

    val nowMilli = 100

    val nextAction = mock[Action]
    val id: ExecutionId = ExecutionId(UUID.randomUUID().toString)
    val idExpression: Expression[ExecutionId] = _ => id
    val state = mock[PendingRequestsState]
    val session = emptySession
    val configuration = GatlingConfiguration.loadForTest()

    val clock = new Clock {
      override def nowMillis: Long = nowMilli
    }

    val coreComponents =
      new CoreComponents(system, mock[EventLoopGroup], mock[ActorRef], Option(mock[Throttler]), mock[StatsEngine], clock, mock[Action], configuration)
    val protocolComponentsRegistry = new ProtocolComponentsRegistries(coreComponents, Map.empty).scenarioRegistry(Map.empty)
    val scenario = new ScenarioContext(coreComponents, protocolComponentsRegistry, Constant, throttled = false)

  }

}
