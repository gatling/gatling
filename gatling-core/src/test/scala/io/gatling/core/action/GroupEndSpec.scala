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
import io.gatling.core.stats.writer.GroupMessage

import akka.testkit._

class GroupEndSpec extends AkkaSpec {

  private val clock = new DefaultClock

  "GroupEnd" should "exit the current group" in {
    val dataWriterProbe = TestProbe()
    val statsEngine = new DataWritersStatsEngine(List(dataWriterProbe.ref), system, clock)

    val groupEnd = new GroupEnd(statsEngine, clock, new ActorDelegatingAction("next", self))

    val session = Session("scenario", 0, clock.nowMillis)
    val sessionInGroup = session.enterGroup("group", clock.nowMillis)

    groupEnd ! sessionInGroup
    expectMsg(session)

    dataWriterProbe.expectMsgType[GroupMessage]
  }
}
