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

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.core.action.{ Action, Counter }
import io.gatling.core.session.{ Expression, StaticValueExpression }
import io.gatling.core.structure.ScenarioContext

private[gatling] object CounterBuilder {
  def apply(key: String): CounterBuilder =
    new CounterBuilder(
      key,
      start = StaticValueExpression(0),
      increment = StaticValueExpression(1),
      end = StaticValueExpression(Int.MaxValue),
      wrap = false,
      tracksPerUser = false,
      sharded = false
    )
}

private[gatling] final class CounterBuilder(
    key: String,
    start: Expression[Int],
    increment: Expression[Int],
    end: Expression[Int],
    wrap: Boolean,
    tracksPerUser: Boolean,
    sharded: Boolean
) extends ActionBuilder {
  // shared amongst all the Actions built from this very builder instance, so that using the same builder
  // in multiple places of a Simulation gives one single sequence of values, just like feeders
  private lazy val index = new AtomicInteger

  /**
   * Set the first value to be emitted. Must be positive. Default is 0. Can only be dynamic, eg a function or a Gatling EL String, when [[perUser]] is used.
   */
  def startingAt(newStart: Expression[Int]): CounterBuilder = new CounterBuilder(key, newStart, increment, end, wrap, tracksPerUser, sharded)

  /**
   * Set the gap between 2 successive values. Default is 1. Can only be dynamic, eg a function or a Gatling EL String, when [[perUser]] is used.
   */
  def withIncrement(newIncrement: Expression[Int]): CounterBuilder = new CounterBuilder(key, start, newIncrement, end, wrap, tracksPerUser, sharded)

  /**
   * Set the inclusive upper bound. Default is Int.MaxValue. Once it's reached, the load generator is stopped, unless [[wrapAround]] is used. Can only be
   * dynamic, eg a function or a Gatling EL String, when [[perUser]] is used.
   */
  def upTo(newEnd: Expression[Int]): CounterBuilder = new CounterBuilder(key, start, increment, newEnd, wrap, tracksPerUser, sharded)

  /**
   * Start over from the first value once the upper bound is reached, instead of stopping the load generator. Beware values are then no longer unique.
   */
  def wrapAround: CounterBuilder = new CounterBuilder(key, start, increment, end, wrap = true, tracksPerUser, sharded)

  /**
   * Track the counter independently for each virtual user, so they all get the very same sequence of values, instead of sharing one single sequence.
   */
  def perUser: CounterBuilder = new CounterBuilder(key, start, increment, end, wrap, tracksPerUser = true, sharded)

  /**
   * Distribute the values evenly amongst all the load generators of a Gatling Enterprise cluster, so they remain unique cluster wide. Only effective when the
   * test is running with Gatling Enterprise, noop otherwise. Each load generator only gets a slice of the range, so the values emitted by the whole cluster
   * have holes, which is the price to pay for not having to synchronize the load generators.
   */
  def shard: CounterBuilder = new CounterBuilder(key, start, increment, end, wrap, tracksPerUser, sharded = true)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    require(!(tracksPerUser && sharded), s"Counter '$key' can't be both perUser and shard as per user counters are not shared in the first place")

    (start, increment, end) match {
      case (StaticValueExpression(startValue), StaticValueExpression(incrementValue), StaticValueExpression(endValue)) =>
        Counter.validateRange(key, startValue, incrementValue, endValue).onFailure(message => throw new IllegalArgumentException(message))

        val count = Counter.valueCount(startValue, incrementValue, endValue)

        val (first: Int, length: Long) =
          (startValue, count)

        if (tracksPerUser) {
          new Counter.PerUser(key, first, incrementValue, length, wrap, ctx.coreComponents.controller, ctx.coreComponents.statsEngine, next)
        } else {
          new Counter.Shared(key, first, incrementValue, length, wrap, index, ctx.coreComponents.controller, ctx.coreComponents.statsEngine, next)
        }

      case _ =>
        require(
          tracksPerUser,
          s"Counter '$key' can only use dynamic startingAt, withIncrement and upTo values, such as functions and Gatling EL Strings, when perUser is used"
        )

        new Counter.PerUserDynamic(key, start, increment, end, wrap, ctx.coreComponents.controller, ctx.coreComponents.statsEngine, next)
    }
  }
}
