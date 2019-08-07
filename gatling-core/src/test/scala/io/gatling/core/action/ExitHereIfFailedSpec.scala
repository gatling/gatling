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

package io.gatling.core.action

import io.gatling.AkkaSpec
import io.gatling.commons.util.DefaultClock
import io.gatling.core.session.Session
import io.gatling.core.stats.DataWritersStatsEngine
import io.gatling.core.stats.writer.{ GroupMessage, Init, RunMessage }

import akka.testkit._

class ExitHereIfFailedSpec extends AkkaSpec {

  private val clock = new DefaultClock
  private val nextAction = mock[Action]

  def newExitHereIfFailed(exitProbe: TestProbe, dataWriterProbe: TestProbe): ExitHereIfFailed = {
    val statsEngine = new DataWritersStatsEngine(
      Init(
        Nil,
        RunMessage(
          "simulationClassName",
          "simulationId",
          0,
          "runDescription",
          "gatlingVersion"
        ),
        Nil
      ),
      List(dataWriterProbe.ref), system, clock
    )
    val exit = new ActorDelegatingAction("exit", exitProbe.ref)

    new ExitHereIfFailed(exit, statsEngine, clock, new ActorDelegatingAction("next", self))
  }

  "ExitHereIfFailed" should "send the session to the next actor if the session was not failed" in {
    val exitProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = newExitHereIfFailed(exitProbe, dataWriterProbe)

    val session = Session("scenario", 0, clock.nowMillis)

    exitHereIfFailed ! session

    expectMsg(session)
    exitProbe.expectNoMessage(remainingOrDefault)
  }

  it should "end the scenario by sending the session to the user end if the session failed" in {
    val exitProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = newExitHereIfFailed(exitProbe, dataWriterProbe)

    val session = Session("scenario", 0, clock.nowMillis).enterTryMax("loop", nextAction).markAsFailed

    exitHereIfFailed ! session

    expectNoMessage(remainingOrDefault)
    exitProbe.expectMsg(session)
  }

  it should "also log a group end if the user was inside a group" in {
    val exitProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = newExitHereIfFailed(exitProbe, dataWriterProbe)

    val session = Session("scenario", 0, clock.nowMillis).enterGroup("group", clock.nowMillis).markAsFailed

    exitHereIfFailed ! session

    expectNoMessage(remainingOrDefault)
    exitProbe.expectMsg(session)
    dataWriterProbe.expectMsgType[GroupMessage]
  }
}
