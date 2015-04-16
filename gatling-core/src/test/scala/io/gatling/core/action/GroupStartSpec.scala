/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import akka.testkit._
import io.gatling.AkkaSpec

import io.gatling.core.session.el.El
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session.{ GroupBlock, Session }

class GroupStartSpec extends AkkaSpec {

  "GroupStart" should "resolve the group name from the session and create a new group" in {
    val dataWriterProbe = TestProbe()
    val dataWriters = new DataWriters(system, List(dataWriterProbe.ref))
    val groupExpr = "${theGroupName}".el[String]

    val groupStart = TestActorRef(GroupStart.props(groupExpr, dataWriters, self))

    val session = Session("scenario", "userId", attributes = Map("theGroupName" -> "foo"))

    groupStart ! session

    val sessionInGroup = expectMsgType[Session]
    sessionInGroup.blockStack.head shouldBe an[GroupBlock]
    sessionInGroup.blockStack.head.asInstanceOf[GroupBlock].hierarchy shouldBe List("foo")
  }
}
