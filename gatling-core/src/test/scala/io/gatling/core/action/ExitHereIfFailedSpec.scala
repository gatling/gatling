/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.action

import akka.actor.ActorRef
import akka.testkit._

import io.gatling.AkkaSpec
import io.gatling.core.result.writer.{ GroupMessage, DataWriters }
import io.gatling.core.session.Session

class ExitHereIfFailedSpec extends AkkaSpec {

  def createExitHereIfFailed(userEndProbe: TestProbe, datawriterProbe: TestProbe) =
    TestActorRef(ExitHereIfFailed.props(userEndProbe.ref, new DataWriters(system, List(datawriterProbe.ref)), self))

  "ExitHereIfFailed" should "send the session to the next actor if the session was not failed" in {
    val userEndProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = createExitHereIfFailed(userEndProbe, dataWriterProbe)

    val session = Session("scenario", "userId")

    exitHereIfFailed ! session

    expectMsg(session)
    userEndProbe.expectNoMsg()
  }

  it should "end the scenario by sending the session to the user end if the session failed" in {
    val userEndProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = createExitHereIfFailed(userEndProbe, dataWriterProbe)

    val session = Session("scenario", "userId").enterTryMax("loop", ActorRef.noSender).markAsFailed

    exitHereIfFailed ! session

    expectNoMsg()
    userEndProbe.expectMsg(session)
  }

  it should "also log a group end if the user was inside a group" in {
    val userEndProbe = TestProbe()
    val dataWriterProbe = TestProbe()
    val exitHereIfFailed = createExitHereIfFailed(userEndProbe, dataWriterProbe)

    val session = Session("scenario", "userId").enterGroup("group").markAsFailed

    exitHereIfFailed ! session

    expectNoMsg()
    userEndProbe.expectMsg(session)
    dataWriterProbe.expectMsgType[GroupMessage]
  }
}
