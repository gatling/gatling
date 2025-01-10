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

import scala.util.control.NoStackTrace

import io.gatling.commons.stats.Status
import io.gatling.commons.validation._
import io.gatling.core.EmptySession
import io.gatling.core.actor.ActorRef
import io.gatling.core.controller.Controller
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.StatsEngine

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class ChainableActionSpec extends AnyFlatSpecLike with Matchers with EmptySession {

  private val noopStatsEngine = new StatsEngine {
    override private[gatling] def start(): Unit = {}

    override private[gatling] def stop(controller: ActorRef[Controller.Command], exception: Option[Exception]): Unit = {}

    override def logUserStart(scenario: String): Unit = {}

    override def logUserEnd(scenario: String): Unit = {}

    override def logResponse(
        scenario: String,
        groups: List[String],
        requestName: String,
        startTimestamp: Long,
        endTimestamp: Long,
        status: Status,
        responseCode: Option[String],
        message: Option[String]
    ): Unit = {}

    override def logGroupEnd(scenario: String, groupBlock: GroupBlock, exitTimestamp: Long): Unit = {}

    override def logRequestCrash(scenario: String, groups: List[String], requestName: String, error: String): Unit = {}
  }

  class ChainableTestAction(val next: Action, fail: Boolean) extends ChainableAction {
    var hasRun = false

    override val name: String = "chainable"
    override def statsEngine: StatsEngine = noopStatsEngine
    override def execute(session: Session): Unit =
      if (fail) throw new Exception("expected crash") with NoStackTrace
      else hasRun = true
  }

  class NextTestAction extends Action {
    var message: Session = _
    override val name = "next"
    override def execute(session: Session): Unit = message = session
  }

  class FailableTestAction(val next: Action, fail: Boolean) extends ChainableAction {
    var hasRun = false

    override val name: String = "test"
    override def statsEngine: StatsEngine = noopStatsEngine
    override def execute(session: Session): Unit = recover(session) {
      if (fail) {
        "woops".failure
      } else {
        hasRun = true
        "".success
      }
    }
  }

  "A Chainable Action" should "call the execute method when receiving a Session" in {
    val next = new NextTestAction
    val testAction = new ChainableTestAction(next, fail = false)

    testAction.hasRun shouldBe false
    testAction ! emptySession
    testAction.hasRun shouldBe true
  }

  it should "send the session, failed, to the next actor when crashing" in {
    val next = new NextTestAction
    val testAction = new ChainableTestAction(next, fail = true)

    testAction ! emptySession
    next.message shouldBe emptySession.markAsFailed
  }

  "A Failable Action" should "call the execute method when receiving a Session" in {
    val next = new NextTestAction
    val testAction = new FailableTestAction(next, fail = false)

    testAction.hasRun shouldBe false

    testAction ! emptySession
    testAction.hasRun shouldBe true
  }

  it should "send the session, failed, to the next actor when recovering a Failure" in {
    val next = new NextTestAction
    val testAction = new FailableTestAction(next, fail = true)

    testAction ! emptySession
    next.message shouldBe emptySession.markAsFailed
  }
}
