/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import io.gatling.core.session.Session
import io.gatling.core.stats.DataWritersStatsEngine
import io.gatling.core.stats.writer.GroupMessage

import akka.testkit._

class ExitHereIfFailedSpec extends AkkaSpec {

  val nextAction = mock[Action]

  def newExitHereIfFailed(exitProbe: TestProbe, datawriterProbe: TestProbe) = {
    val statsEngine = new DataWritersStatsEngine(system, List(datawriterProbe.ref))
    val exit = new ActorDelegatingAction("exit", exitProbe.ref)

    new ExitHereIfFailed(exit, statsEngine, new ActorDelegatingAction("next", self))
  }

  "ExitHereIfFailed" should "send the session to the next actor if the session was not failed" in {
    val exitProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = newExitHereIfFailed(exitProbe, dataWriterProbe)

    val session = Session("scenario", 0)

    exitHereIfFailed ! session

    expectMsg(session)
    exitProbe.expectNoMsg()
  }

  it should "end the scenario by sending the session to the user end if the session failed" in {
    val exitProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = newExitHereIfFailed(exitProbe, dataWriterProbe)

    val session = Session("scenario", 0).enterTryMax("loop", nextAction).markAsFailed

    exitHereIfFailed ! session

    expectNoMsg()
    exitProbe.expectMsg(session)
  }

  it should "also log a group end if the user was inside a group" in {
    val exitProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = newExitHereIfFailed(exitProbe, dataWriterProbe)

    val session = Session("scenario", 0).enterGroup("group").markAsFailed

    exitHereIfFailed ! session

    expectNoMsg()
    exitProbe.expectMsg(session)
    dataWriterProbe.expectMsgType[GroupMessage]
  }
}
