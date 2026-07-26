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

import io.gatling.commons.stats.OK
import io.gatling.core.EmptySession
import io.gatling.core.session._

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class BlockExitSpec extends AnyFlatSpecLike with Matchers with MockitoSugar with EmptySession {
  private val FalseExpressionSuccess: Expression[Boolean] = false.expressionSuccess

  "mustExit" should "not exit when the block stack is empty" in {
    BlockExit.mustExit(emptySession) shouldBe None
  }

  it should "not exit when the block stack only holds groups" in {
    BlockExit.mustExit(emptySession.enterGroup("group", 0)) shouldBe None
  }

  it should "not exit an exitASAP loop whose condition still holds" in {
    val session = emptySession.enterLoop("counter", TrueExpressionSuccess, mock[Action], exitASAP = true)

    BlockExit.mustExit(session) shouldBe None
  }

  it should "not exit a non-exitASAP loop, even when its condition no longer holds" in {
    val session = emptySession.enterLoop("counter", FalseExpressionSuccess, mock[Action], exitASAP = false)

    BlockExit.mustExit(session) shouldBe None
  }

  it should "exit an exitASAP loop whose condition no longer holds" in {
    val exitAction = mock[Action]
    val session = emptySession.enterLoop("counter", FalseExpressionSuccess, exitAction, exitASAP = true)

    val blockExit = BlockExit.mustExit(session).getOrElse(fail("expected the exitASAP loop to exit"))

    blockExit.exitAction shouldBe exitAction
    blockExit.groupsToClose shouldBe empty
    blockExit.session.blockStack shouldBe empty
    blockExit.session.contains("counter") shouldBe false
  }

  it should "close the groups nested inside the exited exitASAP loop" in {
    val exitAction = mock[Action]
    val session = emptySession
      .enterLoop("counter", FalseExpressionSuccess, exitAction, exitASAP = true)
      .enterGroup("group", 0)

    val blockExit = BlockExit.mustExit(session).getOrElse(fail("expected the exitASAP loop to exit"))

    blockExit.exitAction shouldBe exitAction
    blockExit.groupsToClose shouldBe List(GroupBlock(List("group"), 0, 0, OK))
    blockExit.session.blockStack shouldBe empty
  }

  it should "leave the enclosing group open when the exited exitASAP loop is nested inside it" in {
    val exitAction = mock[Action]
    val session = emptySession
      .enterGroup("group", 0)
      .enterLoop("counter", FalseExpressionSuccess, exitAction, exitASAP = true)

    val blockExit = BlockExit.mustExit(session).getOrElse(fail("expected the exitASAP loop to exit"))

    blockExit.groupsToClose shouldBe empty
    blockExit.session.blockStack shouldBe List(GroupBlock(List("group"), 0, 0, OK))
  }

  it should "exit a failed tryMax" in {
    val tryMaxAction = mock[Action]
    val session = emptySession.enterTryMax("counter", tryMaxAction).markAsFailed

    val blockExit = BlockExit.mustExit(session).getOrElse(fail("expected the failed tryMax to exit"))

    blockExit.exitAction shouldBe tryMaxAction
  }

  it should "give exitASAP loops precedence over failed tryMax" in {
    val loopExitAction = mock[Action]
    val session = emptySession
      .enterTryMax("tryMaxCounter", mock[Action])
      .enterLoop("loopCounter", FalseExpressionSuccess, loopExitAction, exitASAP = true)
      .markAsFailed

    val blockExit = BlockExit.mustExit(session).getOrElse(fail("expected the exitASAP loop to exit"))

    blockExit.exitAction shouldBe loopExitAction
  }
}
