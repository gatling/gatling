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
package io.gatling.core.structure

import akka.actor.ActorRef
import io.gatling.core.CoreModule
import io.gatling.core.pause.Constant
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.test.ActorSupport
import io.gatling.core.config.{ GatlingConfiguration, Protocols }
import io.gatling.core.session.Session

class ExecsSpec extends FlatSpec with Matchers with MockitoSugar with CoreModule {

  implicit val configuration = GatlingConfiguration.loadForTest()
  val ctx = ScenarioContext(mock[ActorRef], mock[ActorRef], Protocols(), Constant, throttled = false)

  "Execs" should "wrap Scenarios in chains, using exec" in ActorSupport { testKit =>

    import testKit._
    val testScenario = scenario("Test Scenario").exec { session =>
      self ! "Message 2"
      session
    }

    val chainBuilder = exec { session =>
      self ! "Message 1"
      session
    }
      .exec(testScenario)
      .exec { session =>
        self ! "Message 3"
        session
      }

    val chain = chainBuilder.build(self, ctx)
    val session = Session("TestScenario", "testUser")
    chain ! session
    /*
     * We're cheating slightly by assuming messages will be delivered
     * in order (technically, Akka doesn't guarantee transitive
     * ordering, although within the same JVM ordering is generally
     * transitive) as it gives us more informative error messages.
     */
    expectMsg("Message 1")
    expectMsg("Message 2")
    expectMsg("Message 3")
    expectMsg(session)
    expectNoMsg()
  }
}
