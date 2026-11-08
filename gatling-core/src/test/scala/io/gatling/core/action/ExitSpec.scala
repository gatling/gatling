/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.core.EmptySession
import io.gatling.core.actor.ActorSpec
import io.gatling.core.controller.inject.Injector
import io.gatling.core.protocol.{ ExitHook, ProtocolComponents }
import io.gatling.core.session.Session

class ExitSpec extends ActorSpec with EmptySession {
  "Exit" should "terminate the session and notify the Controller execution has ended" in {
    var hasTerminated = false

    val mockInjector = mockActorRef[Injector.Command]("injector")

    val exit = new Exit(mockInjector)
    val session = emptySession.copy(onExit = _ => hasTerminated = true)
    exit ! session

    hasTerminated shouldBe true
    val userMessage = mockInjector.expectMsgType[Injector.Command.UserEnd]()
    userMessage.scenario shouldBe session.scenario
  }

  it should "only notify the Injector once every protocol component is done with the user" in {
    val mockInjector = mockActorRef[Injector.Command]("injector")
    var pendingDone: Option[() => Unit] = None
    var syncExits = 0

    val asyncComponents = new ProtocolComponents {
      override def onStart: Session => Session = Session.Identity
      override def onExit: Session => Unit = _ => ()
      override def onExitAsync(session: Session, done: () => Unit): Unit = pendingDone = Some(done)
    }
    val syncComponents = new ProtocolComponents {
      override def onStart: Session => Session = Session.Identity
      override def onExit: Session => Unit = _ => syncExits += 1
    }

    val exit = new Exit(mockInjector)
    exit ! emptySession.copy(onExit = new ExitHook(() => List(asyncComponents, syncComponents)))

    syncExits shouldBe 1
    mockInjector.expectNoMsg()
    pendingDone.foreach(_())
    mockInjector.expectMsgType[Injector.Command.UserEnd]()
  }
}
