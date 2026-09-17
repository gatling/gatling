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

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.commons.validation._
import io.gatling.core.actor.ActorRef
import io.gatling.core.controller.Controller
import io.gatling.core.session.{ Expression, Session, SessionPrivateAttributes }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

private[core] object Counter {

  /**
   * The number of values in the [start, end) range, walked with the given increment. The upper bound is exclusive, except when it's Int.MaxValue, so that the
   * default range can span over all the positive Ints. Doesn't fit in an Int for this very reason.
   */
  def valueCount(start: Int, increment: Int, end: Int): Long = {
    val span = if (end == Int.MaxValue) end.toLong - start + 1 else end.toLong - start
    // round up so the last value is the greatest one that's still in the range
    (span + increment - 1) / increment
  }

  /**
   * Check the range is usable, shared between the build time check of the static values and the runtime check of the dynamic ones.
   */
  def validateRange(key: String, start: Int, increment: Int, end: Int): Validation[Unit] =
    if (start < 0) {
      s"Counter '$key' start value must be positive but was $start".failure
    } else if (increment <= 0) {
      s"Counter '$key' increment must be strictly positive but was $increment".failure
    } else if (end <= start && end != Int.MaxValue) {
      s"Counter '$key' exclusive upper bound ($end) must be greater than its start value ($start)".failure
    } else {
      Validation.unit
    }

  /**
   * A [[Counter]] whose values are shared amongst all the virtual users of this load generator.
   *
   * @param index
   *   the shared cursor, owned by the CounterBuilder so that all the Actions it builds share one single sequence of values
   */
  private[core] final class Shared(
      key: String,
      first: Int,
      increment: Int,
      length: Long,
      wrapAround: Boolean,
      index: AtomicInteger,
      controller: ActorRef[Controller.Command],
      statsEngine: StatsEngine,
      next: Action
  ) extends Counter(key, wrapAround, controller, statsEngine, next) {
    override def execute(session: Session): Unit = emit(index.getAndIncrement(), first, increment, length, session)
  }

  /**
   * A [[Counter]] that's tracked independently for each virtual user, so they all get the very same sequence of values.
   */
  private[core] final class PerUser(
      key: String,
      first: Int,
      increment: Int,
      length: Long,
      wrapAround: Boolean,
      controller: ActorRef[Controller.Command],
      statsEngine: StatsEngine,
      next: Action
  ) extends Counter(key, wrapAround, controller, statsEngine, next) {
    private val indexKey = SessionPrivateAttributes.generatePrivateAttribute(s"counter.$key")

    override def execute(session: Session): Unit = {
      val index = session.attributes.get(indexKey).fold(0L)(_.asInstanceOf[Long])
      emit(index, first, increment, length, session.set(indexKey, index + 1))
    }
  }

  /**
   * The range a virtual user walks, resolved once and then stored in its Session.
   */
  private final class ResolvedRange(val first: Int, val increment: Int, val length: Long)

  /**
   * A per virtual user [[Counter]] whose range is resolved from the Session, so each virtual user can get its own one. Only makes sense when tracking per user,
   * as a range resolved from one virtual user's Session can't define the sequence shared by all of them.
   *
   * The range is resolved once per virtual user, on its first pass, and then kept in its Session, so the sequence can't shift under it if the values the range
   * is computed from change later on.
   */
  private[core] final class PerUserDynamic(
      key: String,
      start: Expression[Int],
      increment: Expression[Int],
      end: Expression[Int],
      wrapAround: Boolean,
      controller: ActorRef[Controller.Command],
      statsEngine: StatsEngine,
      next: Action
  ) extends Counter(key, wrapAround, controller, statsEngine, next) {
    private val indexKey = SessionPrivateAttributes.generatePrivateAttribute(s"counter.$key")
    private val rangeKey = SessionPrivateAttributes.generatePrivateAttribute(s"counter.$key.range")

    private def emitFrom(range: ResolvedRange, session: Session): Unit = {
      val index = session.attributes.get(indexKey).fold(0L)(_.asInstanceOf[Long])
      emit(index, range.first, range.increment, range.length, session.set(indexKey, index + 1))
    }

    override def execute(session: Session): Unit = recover(session) {
      session.attributes.get(rangeKey) match {
        case Some(range) =>
          emitFrom(range.asInstanceOf[ResolvedRange], session)
          Validation.unit

        case _ =>
          for {
            startValue <- start(session)
            incrementValue <- increment(session)
            endValue <- end(session)
            _ <- validateRange(key, startValue, incrementValue, endValue)
          } yield {
            val range = new ResolvedRange(startValue, incrementValue, valueCount(startValue, incrementValue, endValue))
            emitFrom(range, session.set(rangeKey, range))
          }
      }
    }
  }
}

/**
 * Stores an incrementing value into the Session on each pass.
 *
 * Values are computed from an index instead of being accumulated, so that wrapping around is a mere modulo and so that we can't overflow, whatever the number
 * of passes.
 *
 * @param key
 *   the name of the Session attribute the value is stored into
 * @param wrapAround
 *   if the Counter must start over instead of stopping the load generator once it has emitted all the values of its range
 */
private[core] sealed abstract class Counter(
    key: String,
    wrapAround: Boolean,
    controller: ActorRef[Controller.Command],
    override val statsEngine: StatsEngine,
    override val next: Action
) extends ChainableAction
    with NameGen {
  override val name: String = genName("counter")

  /**
   * @param index
   *   the number of values already emitted by this sequence
   * @param first
   *   the first value to be emitted, already offset when sharding is effective
   * @param increment
   *   the gap between 2 successive values
   * @param length
   *   the number of values this sequence is allowed to emit
   */
  protected def emit(index: Long, first: Int, increment: Int, length: Long, session: Session): Unit =
    if (!wrapAround && index >= length) {
      controller ! Controller.Command.StopLoadGenerator(
        Controller.Command.StopLoadGenerator.Reason.Crash.WellKnown(
          s"Counter '$key' has exhausted its $length values, either widen its range with upTo or make it wrapAround"
        )
      )
    } else {
      val rank = if (wrapAround) index % length else index
      // can't overflow as the last value is the upper bound, which is an Int
      next ! session.set(key, (first + rank * increment).toInt)
    }
}
