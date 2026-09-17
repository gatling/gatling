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

package io.gatling.core.action.builder

import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.action.Counter
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.pause.Constant
import io.gatling.core.session.{ Expression, ExpressionSuccessWrapper }
import io.gatling.core.structure.ScenarioContext

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CounterBuilderSpec extends AnyFlatSpec with Matchers {
  private val ctx = {
    val coreComponents = new CoreComponents(null, null, null, None, null, null, null, GatlingConfiguration.loadForTest())
    new ScenarioContext(coreComponents, null, Constant, throttled = false)
  }

  private val dynamic: Expression[Int] = _ => 1.success

  "CounterBuilder" should "build a shared counter by default" in {
    CounterBuilder("counter").build(ctx, null) shouldBe a[Counter.Shared]
  }

  it should "build a per user counter when perUser is used" in {
    CounterBuilder("counter").perUser.build(ctx, null) shouldBe a[Counter.PerUser]
  }

  it should "build a dynamic per user counter when perUser is used together with dynamic values" in {
    CounterBuilder("counter").startingAt(dynamic).perUser.build(ctx, null) shouldBe a[Counter.PerUserDynamic]
    CounterBuilder("counter").withIncrement(dynamic).perUser.build(ctx, null) shouldBe a[Counter.PerUserDynamic]
    CounterBuilder("counter").upTo(dynamic).perUser.build(ctx, null) shouldBe a[Counter.PerUserDynamic]
  }

  it should "fail when a dynamic startingAt is used without perUser" in {
    an[IllegalArgumentException] should be thrownBy CounterBuilder("counter").startingAt(dynamic).build(ctx, null)
  }

  it should "fail when a dynamic withIncrement is used without perUser" in {
    an[IllegalArgumentException] should be thrownBy CounterBuilder("counter").withIncrement(dynamic).build(ctx, null)
  }

  it should "fail when a dynamic upTo is used without perUser" in {
    an[IllegalArgumentException] should be thrownBy CounterBuilder("counter").upTo(dynamic).build(ctx, null)
  }

  it should "fail when a dynamic value is used together with shard" in {
    an[IllegalArgumentException] should be thrownBy CounterBuilder("counter").startingAt(dynamic).shard.build(ctx, null)
  }

  it should "still validate the static values" in {
    an[IllegalArgumentException] should be thrownBy CounterBuilder("counter").startingAt(-1.expressionSuccess).build(ctx, null)
    an[IllegalArgumentException] should be thrownBy CounterBuilder("counter").withIncrement(0.expressionSuccess).build(ctx, null)
    an[IllegalArgumentException] should be thrownBy CounterBuilder("counter").startingAt(10.expressionSuccess).upTo(5.expressionSuccess).build(ctx, null)
  }
}
