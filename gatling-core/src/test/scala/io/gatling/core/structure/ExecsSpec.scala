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
package io.gatling.core.structure

import io.gatling.AkkaSpec
import io.gatling.core.{ CoreComponents, CoreDsl }
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.pause.Constant
import io.gatling.core.protocol.{ ProtocolComponentsRegistries, Protocols }
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine

import akka.actor.ActorRef

class ExecsSpec extends AkkaSpec with CoreDsl {

  implicit val configuration = GatlingConfiguration.loadForTest()
  val coreComponents = CoreComponents(mock[ActorRef], mock[Throttler], mock[StatsEngine], mock[Action], configuration)
  val protocolComponentsRegistry = new ProtocolComponentsRegistries(system, coreComponents, Protocols(Nil)).scenarioRegistry(Protocols(Nil))
  val ctx = ScenarioContext(system, coreComponents, protocolComponentsRegistry, Constant, throttled = false)

  "Execs" should "wrap Scenarios in chains, using exec" in {

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

    val chain = chainBuilder.build(ctx, new Action {
      override def execute(session: Session): Unit = self ! session
      override def name: String = "loop"
    })
    val session = Session("TestScenario", 0)
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
