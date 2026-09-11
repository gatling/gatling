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

import java.{ util => ju }

import scala.concurrent.duration.{ Duration, DurationInt, FiniteDuration }
import scala.jdk.CollectionConverters._

import io.gatling.core.action.{ Action, SharedQueueActor, SharedQueuePoll, SharedQueuePut, SharedQueueSize, SharedQueueTake }
import io.gatling.core.actor.ActorRef
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

object SharedQueueBuilder {
  private val Instances = new ju.HashMap[Long, ActorRef[SharedQueueActor.Command]].asScala

  private[gatling] def apply(name: String): SharedQueueBuilder = new SharedQueueBuilder(name)
}

/**
 * A factory of actions that exchange values over an in-memory queue, so virtual users can communicate with each other.
 *
 * The queue is owned by this very builder instance, so that all the actions it builds share one single queue, just like feeders. Store it in a `val` and use
 * that same `val` everywhere the queue must be accessed.
 *
 * The queue is local to this load generator: when running a distributed test with Gatling Enterprise, each load generator has its own queue and they don't
 * exchange values with each other.
 */
final class SharedQueueBuilder private (private[builder] val name: String) extends NameGen {
  private[builder] def actor(ctx: ScenarioContext): ActorRef[SharedQueueActor.Command] =
    SharedQueueBuilder.Instances.getOrElseUpdate(
      System.identityHashCode(this),
      ctx.coreComponents.actorSystem.actorOf(
        new SharedQueueActor(name, ctx.coreComponents.clock, SharedQueueActor.SweepPeriod, genName("sharedQueue"))
      )
    )

  /**
   * Bootstrap a builder for an action that stores a value into this queue, then moves on without waiting.
   *
   * @param value
   *   the value to be enqueued
   */
  def put(value: Expression[Any]): ActionBuilder = new SharedQueuePutBuilder(this, value)

  /**
   * Bootstrap a builder for an action that pops the oldest value of this queue into the virtual user's Session. If the queue is empty, the virtual user waits
   * until a value is available, possibly forever unless [[SharedQueueTakeBuilder.timeout]] is used.
   *
   * @param key
   *   the name of the Session attribute the value is stored into
   */
  def take(key: String): SharedQueueTakeBuilder = new SharedQueueTakeBuilder(this, key, None)

  /**
   * Bootstrap a builder for an action that pops the oldest value of this queue into the virtual user's Session. If the queue is empty, the virtual user is
   * marked as failed and moves on instead of waiting, and the Session attribute is left untouched.
   *
   * @param key
   *   the name of the Session attribute the value is stored into
   */
  def poll(key: String): ActionBuilder = new SharedQueuePollBuilder(this, key)

  /**
   * Bootstrap a builder for an action that stores the current number of values in this queue into the virtual user's Session.
   *
   * @param key
   *   the name of the Session attribute the size is stored into
   */
  def size(key: String): ActionBuilder = new SharedQueueSizeBuilder(this, key)
}

private[builder] final class SharedQueuePutBuilder(queue: SharedQueueBuilder, value: Expression[Any]) extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action =
    new SharedQueuePut(queue.actor(ctx), queue.name, value, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
}

/**
 * Builder of an action that pops the oldest value of a queue into the virtual user's Session, waiting for one to be available if the queue is empty.
 *
 * Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
final class SharedQueueTakeBuilder private[builder] (queue: SharedQueueBuilder, key: String, timeout: Option[FiniteDuration]) extends ActionBuilder {

  /**
   * Give up after the given duration instead of waiting forever. The virtual user is then marked as failed and moves on, and the Session attribute is left
   * untouched.
   *
   * Timeouts are only meant to avoid stalling virtual users forever, so they're best effort: expired virtual users are released periodically, hence they can
   * wait up to one extra second.
   */
  def timeout(newTimeout: FiniteDuration): SharedQueueTakeBuilder = new SharedQueueTakeBuilder(queue, key, Some(newTimeout))

  /**
   * Give up after the given number of seconds instead of waiting forever. The virtual user is then marked as failed and moves on, and the Session attribute is
   * left untouched.
   *
   * Timeouts are only meant to avoid stalling virtual users forever, so they're best effort: expired virtual users are released periodically, hence they can
   * wait up to one extra second.
   */
  def timeout(newTimeoutSeconds: Int): SharedQueueTakeBuilder = timeout(newTimeoutSeconds.seconds)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    require(timeout.forall(_ > Duration.Zero), s"Queue take '$key' timeout must be strictly positive but was ${timeout.getOrElse(Duration.Zero)}")
    new SharedQueueTake(queue.actor(ctx), key, timeout, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
  }
}

private[builder] final class SharedQueuePollBuilder(queue: SharedQueueBuilder, key: String) extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action =
    new SharedQueuePoll(queue.actor(ctx), key, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
}

private[builder] final class SharedQueueSizeBuilder(queue: SharedQueueBuilder, key: String) extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action =
    new SharedQueueSize(queue.actor(ctx), key, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
}
