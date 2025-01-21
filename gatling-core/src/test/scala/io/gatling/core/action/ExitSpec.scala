/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
}
