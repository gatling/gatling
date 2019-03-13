/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.DefaultClock
import io.gatling.core.session.el.El
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.StatsEngine

import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.{ FlatSpec, GivenWhenThen, Matchers }
import org.scalatestplus.mockito.MockitoSugar

class GroupStartSpec extends FlatSpec with Matchers with MockitoSugar with GivenWhenThen {

  private val clock = new DefaultClock

  "GroupStart" should "resolve the group name from the session and create a new group" in {

    Given("a GroupStart Action with a dynamic name")
    val next = mock[Action]
    val groupStart = new GroupStart("${theGroupName}".el[String], mock[StatsEngine], clock, next)

    When("being sent a Session that resolves the group name")
    val session = Session("scenario", 0, clock.nowMillis, attributes = Map("theGroupName" -> "foo"))
    groupStart ! session

    Then("next Action should receive a Session")
    val captor: ArgumentCaptor[Session] = ArgumentCaptor.forClass(classOf[Session])
    verify(next) ! captor.capture()
    val nextSession = captor.getValue

    And("this Session's blockStack should match the resolved group name")
    val nextBlock = nextSession.blockStack.head
    nextBlock shouldBe an[GroupBlock]
    nextBlock.asInstanceOf[GroupBlock].hierarchy shouldBe List("foo")
  }
}
