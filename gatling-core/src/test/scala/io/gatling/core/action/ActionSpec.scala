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
import io.gatling.core.session.Session

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class ActionSpec extends AnyFlatSpecLike with Matchers with EmptySession {
  class TestAction extends Action {
    var hasRun = false

    override val name = "test"
    override def execute(session: Session): Unit = hasRun = true
  }

  "An Action" should "call the execute method when receiving a Session" in {
    val testAction = new TestAction

    testAction.hasRun shouldBe false
    testAction ! emptySession
    testAction.hasRun shouldBe true
  }
}
