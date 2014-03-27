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

import java.util.concurrent.TimeUnit.SECONDS

import scala.concurrent.duration.Duration

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.core.Predef.{ pace, value2Expression }
import io.gatling.core.config.Protocols
import io.gatling.core.session.Session
import io.gatling.core.test.ActorSupport

@RunWith(classOf[JUnitRunner])
class PaceSpec extends Specification {

  "pace" should {
    "run actions with a minimum wait time" in ActorSupport { testKit =>
      import testKit._
      val instance = pace(Duration(3, SECONDS), "paceCounter").build(self, Protocols())

      // Send session, expect response near-instantly
      instance ! Session("TestScenario", "testUser")
      val session1 = expectMsgClass(Duration(1, SECONDS), classOf[Session])

      // Send second session, expect nothing for 7 seconds, then a response
      instance ! session1
      expectNoMsg(Duration(2, SECONDS))
      val session2 = expectMsgClass(Duration(2, SECONDS), classOf[Session])

      // counter must have incremented by 3 seconds
      session2("paceCounter").as[Long] must_== session1("paceCounter").as[Long] + 3000L
    }

    "run actions immediately if the minimum time has expired" in ActorSupport { testKit =>
      import testKit._
      val instance = pace(Duration(3, SECONDS), "paceCounter").build(self, Protocols())

      // Send session, expect response near-instantly
      instance ! Session("TestScenario", "testUser")
      val session1 = expectMsgClass(Duration(1, SECONDS), classOf[Session])

      // Wait 3 seconds - simulate overrunning action
      Thread.sleep(3000L)

      // Send second session, expect response near-instantly
      instance ! session1
      val session2 = expectMsgClass(Duration(1, SECONDS), classOf[Session])

      // counter must have incremented by 3 seconds
      session2("paceCounter").as[Long] must_== session1("paceCounter").as[Long] + 3000L
    }
  }
}
