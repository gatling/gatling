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

import io.gatling.core.action.{ Action, Counter }
import io.gatling.core.session.{ Expression, StaticValueExpression }
import io.gatling.core.structure.ScenarioContext

/**
 * Builder of an action that stores an incrementing value into the virtual users' Session, tracked independently for each virtual user. Only reachable from
 * [[CounterBuilder#perUser]], as dynamic values, eg functions and Gatling EL Strings, only make sense when tracking per user: a range resolved from one
 * virtual user's Session can't define the sequence shared by all of them.
 */
private[gatling] final class PerUserCounterBuilder(
    key: String,
    start: Expression[Int],
    increment: Expression[Int],
    end: Expression[Int],
    wrap: Boolean
) extends ActionBuilder {

  /**
   * Set the first value to be emitted. Must be positive. Default is 0.
   */
  def startingAt(newStart: Expression[Int]): PerUserCounterBuilder = new PerUserCounterBuilder(key, newStart, increment, end, wrap)

  /**
   * Set the gap between 2 successive values. Default is 1.
   */
  def withIncrement(newIncrement: Expression[Int]): PerUserCounterBuilder = new PerUserCounterBuilder(key, start, newIncrement, end, wrap)

  /**
   * Set the upper bound, exclusive, except when it's Int.MaxValue, which is the default. Once it's reached, the load generator is stopped, unless
   * [[wrapAround]] is used.
   */
  def upTo(newEnd: Expression[Int]): PerUserCounterBuilder = new PerUserCounterBuilder(key, start, increment, newEnd, wrap)

  /**
   * Start over from the first value once the upper bound is reached, instead of stopping the load generator. Beware values are then no longer unique.
   */
  def wrapAround: PerUserCounterBuilder = new PerUserCounterBuilder(key, start, increment, end, wrap = true)

  override def build(ctx: ScenarioContext, next: Action): Action =
    (start, increment, end) match {
      case (StaticValueExpression(startValue), StaticValueExpression(incrementValue), StaticValueExpression(endValue)) =>
        Counter.validateRange(key, startValue, incrementValue, endValue).onFailure(message => throw new IllegalArgumentException(message))

        val length = Counter.valueCount(startValue, incrementValue, endValue)

        new Counter.PerUser(key, startValue, incrementValue, length, wrap, ctx.coreComponents.controller, ctx.coreComponents.statsEngine, next)

      case _ =>
        new Counter.PerUserDynamic(key, start, increment, end, wrap, ctx.coreComponents.controller, ctx.coreComponents.statsEngine, next)
    }
}
