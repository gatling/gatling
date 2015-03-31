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

import akka.testkit._
import io.gatling.AkkaSpec

import io.gatling.core.result.writer.{ DataWriters, GroupMessage }
import io.gatling.core.session.Session

class GroupEndSpec extends AkkaSpec {

  "GroupEnd" should "exit the current group" in {
    val dataWriterProbe = TestProbe()
    val dataWriters = new DataWriters(system, List(dataWriterProbe.ref))

    val groupEnd = TestActorRef(GroupEnd.props(dataWriters, self))

    val session = Session("scenario", "userId")
    val sessionInGroup = session.enterGroup("group")

    groupEnd ! sessionInGroup
    expectMsg(session)

    dataWriterProbe.expectMsgType[GroupMessage]
  }
}

