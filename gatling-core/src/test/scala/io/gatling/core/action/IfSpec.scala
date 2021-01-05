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
import io.gatling.core.session.el.El
import io.gatling.core.stats.StatsEngine

import org.mockito.Mockito._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar

class IfSpec extends AnyFlatSpec with MockitoSugar with GivenWhenThen with EmptySession {

  private val clock = new DefaultClock
  private val condition = "${condition}".el[Boolean]

  "If" should "send the session to thenNext when condition evaluates to true" in {

    Given("an If Action")
    val thenNext = mock[Action]
    val elseNext = mock[Action]
    val ifAction = new If(condition, thenNext, elseNext, mock[StatsEngine], clock, mock[Action])

    When("being sent a Session that causes condition to be evaluated as true")
    val session = emptySession.set("condition", true)
    ifAction ! session

    Then("Session should be propagated to thenNext")
    verify(thenNext) ! session

    And("not to elseNext")
    verify(elseNext, never) ! _
  }

  it should "send the session to elseNext when condition evaluates to false" in {

    Given("an IfAction")
    val thenNext = mock[Action]
    val elseNext = mock[Action]
    val ifAction = new If(condition, thenNext, elseNext, mock[StatsEngine], clock, mock[Action])

    When("being sent a Session that causes condition to be evaluated as false")
    val session = emptySession.set("condition", false)
    ifAction ! session

    Then("Session should be propagated to elseNext")
    verify(elseNext) ! session

    And("not to thenNext")
    verify(thenNext, never) ! _
  }
}
