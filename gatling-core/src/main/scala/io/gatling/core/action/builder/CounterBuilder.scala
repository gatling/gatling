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
import io.gatling.core.session.StaticValueExpression
import io.gatling.core.structure.ScenarioContext

private[gatling] object CounterBuilder {
  def apply(key: String): CounterBuilder =
    new CounterBuilder(key, start = 0, increment = 1, end = Int.MaxValue, wrap = false, sharded = false)
}

/**
 * Builder of an action that stores an incrementing value into the virtual users' Session. Values are shared amongst all the virtual users of this load
 * generator, unless [[perUser]] is used, in which case dynamic values become available.
 */
private[gatling] final class CounterBuilder(
    key: String,
    start: Int,
    increment: Int,
    end: Int,
    wrap: Boolean,
    sharded: Boolean
) extends ActionBuilder {
  // shared amongst all the Actions built from this very builder instance, so that using the same builder
  // in multiple places of a Simulation gives one single sequence of values, just like feeders
  private lazy val index = new AtomicInteger

  /**
   * Set the first value to be emitted. Must be positive. Default is 0.
   */
  def startingAt(newStart: Int): CounterBuilder = new CounterBuilder(key, newStart, increment, end, wrap, sharded)

  /**
   * Set the gap between 2 successive values. Default is 1.
   */
  def withIncrement(newIncrement: Int): CounterBuilder = new CounterBuilder(key, start, newIncrement, end, wrap, sharded)

  /**
   * Set the upper bound, exclusive, except when it's Int.MaxValue, which is the default. Once it's reached, the load generator is stopped, unless
   * [[wrapAround]] is used.
   */
  def upTo(newEnd: Int): CounterBuilder = new CounterBuilder(key, start, increment, newEnd, wrap, sharded)

  /**
   * Start over from the first value once the upper bound is reached, instead of stopping the load generator. Beware values are then no longer unique.
   */
  def wrapAround: CounterBuilder = new CounterBuilder(key, start, increment, end, wrap = true, sharded)

  /**
   * Track the counter independently for each virtual user, so they all get the very same sequence of values, instead of sharing one single sequence. Also
   * unlocks dynamic values, eg functions and Gatling EL Strings, for [[PerUserCounterBuilder#startingAt startingAt]],
   * [[PerUserCounterBuilder#withIncrement withIncrement]] and [[PerUserCounterBuilder#upTo upTo]], as a range resolved from one virtual user's Session couldn't
   * define the sequence shared by all of them.
   */
  def perUser: PerUserCounterBuilder =
    new PerUserCounterBuilder(key, StaticValueExpression(start), StaticValueExpression(increment), StaticValueExpression(end), wrap)

  /**
   * Distribute the values evenly amongst all the load generators of a Gatling Enterprise cluster, so they remain unique cluster wide. Only effective when the
   * test is running with Gatling Enterprise, noop otherwise. Each load generator only gets a slice of the range, so the values emitted by the whole cluster
   * have holes, which is the price to pay for not having to synchronize the load generators.
   */
  def shard: CounterBuilder = new CounterBuilder(key, start, increment, end, wrap, sharded = true)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    Counter.validateRange(key, start, increment, end).onFailure(message => throw new IllegalArgumentException(message))

    val count = Counter.valueCount(start, increment, end)

    new Counter.Shared(key, start, increment, count, wrap, index, ctx.coreComponents.controller, ctx.coreComponents.statsEngine, next)
  }
}
