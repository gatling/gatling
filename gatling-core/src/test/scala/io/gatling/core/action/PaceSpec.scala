/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.action

import scala.concurrent.duration._

import akka.actor.ActorDSL.actor

import io.gatling.AkkaSpec
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.Predef.value2Expression
import io.gatling.core.session.Session

class PaceSpec extends AkkaSpec {

  "pace" should "run actions with a minimum wait time" in {
    val instance = actor(new Pace(3.seconds, "paceCounter", mock[DataWriters], self))

    // Send session, expect response near-instantly
    instance ! Session("TestScenario", "testUser")
    val session1 = expectMsgClass(1.second, classOf[Session])

    // Send second session, expect nothing for 7 seconds, then a response
    instance ! session1
    expectNoMsg(2.seconds)
    val session2 = expectMsgClass(2.seconds, classOf[Session])

    // counter must have incremented by 3 seconds
    session2("paceCounter").as[Long] shouldBe session1("paceCounter").as[Long] + 3000L
  }

  it should "run actions immediately if the minimum time has expired" in {
    val instance = actor(new Pace(3.seconds, "paceCounter", mock[DataWriters], self))

    // Send session, expect response near-instantly
    instance ! Session("TestScenario", "testUser")
    val session1 = expectMsgClass(1.second, classOf[Session])

    // Wait 3 seconds - simulate overrunning action
    Thread.sleep(3000L)

    // Send second session, expect response near-instantly
    instance ! session1
    val session2 = expectMsgClass(1.second, classOf[Session])

    // counter must have incremented by 3 seconds
    session2("paceCounter").as[Long] shouldBe session1("paceCounter").as[Long] + 3000L
  }
}
