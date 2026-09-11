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

import java.{ util => ju }

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.actor.{ Actor, ActorRef, Behavior }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

private[core] object SharedQueueActor {

  /**
   * How often expired waiters are evicted. Timeouts are only meant to avoid stalling virtual users forever, so they don't have to be precise, and sweeping in
   * batch saves scheduling one task per parked virtual user onto the ActorSystem's single threaded scheduler.
   */
  private[gatling] val SweepPeriod: FiniteDuration = 1.second

  private val NoDeadline: Long = Long.MaxValue

  sealed trait Command

  object Command {
    final case class Put(session: Session, value: Any, next: Action) extends Command
    final case class Take(session: Session, key: String, timeout: Option[FiniteDuration], next: Action) extends Command
    final case class Poll(session: Session, key: String, next: Action) extends Command
    final case class Size(session: Session, key: String, next: Action) extends Command
    private[action] case object Sweep extends Command
  }

  /**
   * A virtual user parked until a value is available, or until its deadline is past.
   */
  private final class Waiter(val session: Session, val key: String, val deadline: Long, val next: Action)
}

/**
 * Owns the values and the parked virtual users of a queue.
 *
 * Virtual users can't block while waiting for a value, as they run on an event loop that's shared with many other virtual users. Their [[Session]] is parked
 * here instead and handed back to the next [[Action]] once a value is available, just like [[RendezVousActor]] does. As this Actor is single threaded, races
 * between a put, a take and a timeout are resolved for free.
 */
private[core] final class SharedQueueActor(queueName: String, clock: Clock, sweepPeriod: FiniteDuration, name: String)
    extends Actor[SharedQueueActor.Command](name) {
  import SharedQueueActor._
  import SharedQueueActor.Command._

  private val values = new ju.ArrayDeque[Any]
  // keyed by userId, which is unique load generator wide, and a virtual user can only ever be parked once at a time.
  // LinkedHashMap so that waiters are served in the order they arrived, and can be evicted while iterating.
  private val waiters = new ju.LinkedHashMap[Long, Waiter]
  private var sweeping = false

  private def park(session: Session, key: String, timeout: Option[FiniteDuration], next: Action): Unit = {
    val deadline = timeout.fold(NoDeadline)(duration => clock.nowMillis + duration.toMillis)
    if (deadline != NoDeadline && !sweeping) {
      // first virtual user ever to park with a timeout on this queue, start sweeping
      sweeping = true
      scheduler.scheduleAtFixedRate(sweepPeriod)(self ! Sweep)
    }
    waiters.put(session.userId, new Waiter(session, key, deadline, next))
  }

  override def init(): Behavior[Command] = {
    case Put(session, value, next) =>
      if (waiters.isEmpty) {
        values.addLast(value)
      } else {
        val iterator = waiters.values.iterator()
        val waiter = iterator.next()
        iterator.remove()
        waiter.next ! waiter.session.set(waiter.key, value)
      }
      next ! session
      stay

    case Take(session, key, timeout, next) =>
      if (values.isEmpty) {
        park(session, key, timeout, next)
      } else {
        next ! session.set(key, values.pollFirst())
      }
      stay

    case Poll(session, key, next) =>
      if (values.isEmpty) {
        logger.debug(s"Queue '$queueName' is empty, failing poll")
        next ! session.markAsFailed
      } else {
        next ! session.set(key, values.pollFirst())
      }
      stay

    case Size(session, key, next) =>
      next ! session.set(key, values.size)
      stay

    case Sweep =>
      if (!waiters.isEmpty) {
        val now = clock.nowMillis
        val iterator = waiters.values.iterator()
        while (iterator.hasNext) {
          val waiter = iterator.next()
          if (waiter.deadline <= now) {
            iterator.remove()
            logger.debug(s"Virtual user ${waiter.session.userId} timed out waiting for a value from queue '$queueName'")
            waiter.next ! waiter.session.markAsFailed
          }
        }
      }
      stay
  }
}

/**
 * Stores a value into a queue, then moves on without waiting.
 */
private[core] final class SharedQueuePut(
    queueActor: ActorRef[SharedQueueActor.Command],
    queueName: String,
    value: Expression[Any],
    override val statsEngine: StatsEngine,
    override val clock: Clock,
    override val next: Action
) extends ExitableAction
    with NameGen {
  override val name: String = genName("sharedQueuePut")

  private val nullValueFailure = s"Can't put a null value into shared queue '$queueName'".failure

  override def execute(session: Session): Unit = recover(session) {
    value(session).flatMap { resolvedValue =>
      if (resolvedValue == null) {
        nullValueFailure
      } else {
        queueActor ! SharedQueueActor.Command.Put(session, resolvedValue, next)
        Validation.unit
      }
    }
  }
}

/**
 * Pops a value from a queue into the Session, waiting for one to be available if the queue is empty.
 */
private[core] final class SharedQueueTake(
    queueActor: ActorRef[SharedQueueActor.Command],
    key: String,
    timeout: Option[FiniteDuration],
    override val statsEngine: StatsEngine,
    override val clock: Clock,
    override val next: Action
) extends ExitableAction
    with NameGen {
  override val name: String = genName("sharedQueueTake")

  override def execute(session: Session): Unit = queueActor ! SharedQueueActor.Command.Take(session, key, timeout, next)
}

/**
 * Pops a value from a queue into the Session, failing the virtual user instead of waiting if the queue is empty.
 */
private[core] final class SharedQueuePoll(
    queueActor: ActorRef[SharedQueueActor.Command],
    key: String,
    override val statsEngine: StatsEngine,
    override val clock: Clock,
    override val next: Action
) extends ExitableAction
    with NameGen {
  override val name: String = genName("sharedQueuePoll")

  override def execute(session: Session): Unit = queueActor ! SharedQueueActor.Command.Poll(session, key, next)
}

/**
 * Stores the current number of values in a queue into the Session.
 */
private[core] final class SharedQueueSize(
    queueActor: ActorRef[SharedQueueActor.Command],
    key: String,
    override val statsEngine: StatsEngine,
    override val clock: Clock,
    override val next: Action
) extends ExitableAction
    with NameGen {
  override val name: String = genName("sharedQueueSize")

  override def execute(session: Session): Unit = queueActor ! SharedQueueActor.Command.Size(session, key, next)
}
