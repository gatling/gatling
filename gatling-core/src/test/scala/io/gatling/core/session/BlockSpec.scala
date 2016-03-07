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
package io.gatling.core.session

import io.gatling.BaseSpec
import io.gatling.commons.validation._
import io.gatling.core.action.Action

class BlockSpec extends BaseSpec {

  def newSession = Session("scenario", 0)

  "LoopBlock.unapply" should "return the block's counter name if it is a instance of LoopBlock" in {
    LoopBlock.unapply(ExitAsapLoopBlock("counter", true.expressionSuccess, mock[Action])) shouldBe Some("counter")
    LoopBlock.unapply(ExitOnCompleteLoopBlock("counter")) shouldBe Some("counter")
  }

  it should "return None if it isn't an instance of LoopBlock" in {
    LoopBlock.unapply(GroupBlock(List("root group"))) shouldBe None
  }

  "LoopBlock.continue" should "return true if the condition evaluation succeeds and evaluates to true" in {
    val session = newSession.set("foo", 1)
    LoopBlock.continue(session => (session("foo").as[Int] == 1).success, session) shouldBe true
  }

  it should "return false if the condition evaluation succeeds and evaluates to false or if it failed" in {
    val session = newSession.set("foo", 1)
    LoopBlock.continue(session => (session("foo").as[Int] == 0).success, session) shouldBe false

    LoopBlock.continue(session => "failed".failure, session) shouldBe false
  }
}
