/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.core.stats.writer.UserEndMessage

class ExitSpec extends AkkaSpec {

  private val clock = new DefaultClock

  "Exit" should "terminate the session and notify the Controller execution has ended" in {
    val exit = new Exit(self, clock)

    var hasTerminated = false

    val session = emptySession.copy(onExit = _ => hasTerminated = true)
    exit ! session

    hasTerminated shouldBe true
    val userMessage = expectMsgType[UserEndMessage]
    userMessage.scenario shouldBe session.scenario
  }
}
