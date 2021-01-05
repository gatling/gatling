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

import io.gatling.commons.util.DefaultClock
import io.gatling.core.EmptySession
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.StatsEngine

import org.mockito.{ ArgumentCaptor, ArgumentMatchers }
import org.mockito.Mockito._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class GroupEndSpec extends AnyFlatSpec with Matchers with MockitoSugar with GivenWhenThen with EmptySession {

  private val clock = new DefaultClock

  "GroupEnd" should "exit the current group" in {

    Given("a GroupEnd Action")
    val statsEngine = mock[StatsEngine]
    val next = mock[Action]
    val groupEnd = new GroupEnd(statsEngine, clock, next)

    When("being sent a Session that has one single group")
    val session = emptySession.enterGroup("group", clock.nowMillis)
    groupEnd ! session

    Then("next Action should receive a Session with an empty blockStack")
    val sessionCaptor: ArgumentCaptor[Session] = ArgumentCaptor.forClass(classOf[Session])
    verify(next) ! sessionCaptor.capture()
    val nextSession = sessionCaptor.getValue
    nextSession.blockStack shouldBe empty

    And("StatsEngine#logGroupEnd should be notified with the expected group")
    val groupBlockCaptor: ArgumentCaptor[GroupBlock] = ArgumentCaptor.forClass(classOf[GroupBlock])
    verify(statsEngine).logGroupEnd(ArgumentMatchers.any(), groupBlockCaptor.capture(), ArgumentMatchers.anyLong())
    val groupBlock = groupBlockCaptor.getValue

    groupBlock.groups shouldBe List("group")
  }
}
