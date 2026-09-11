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

import io.gatling.core.actor.ActorRef
import io.gatling.core.controller.Controller
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

private[core] object Counter {

  /**
   * The number of values in the [start, end] range, walked with the given increment. Doesn't fit in an Int as the default range spans over all the positive
   * Ints.
   */
  def valueCount(start: Int, increment: Int, end: Int): Long =
    (end.toLong - start) / increment + 1

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
  ) extends Counter(key, first, increment, length, wrapAround, controller, statsEngine, next) {
    override def execute(session: Session): Unit = emit(index.getAndIncrement(), session)
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
  ) extends Counter(key, first, increment, length, wrapAround, controller, statsEngine, next) {
    private val indexKey = SessionPrivateAttributes.generatePrivateAttribute(s"counter.$key")

    override def execute(session: Session): Unit = {
      val index = session.attributes.get(indexKey).fold(0L)(_.asInstanceOf[Long])
      emit(index, session.set(indexKey, index + 1))
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
 * @param first
 *   the first value to be emitted, already offset when sharding is effective
 * @param increment
 *   the gap between 2 successive values
 * @param length
 *   the number of values this Counter is allowed to emit
 * @param wrapAround
 *   if the Counter must start over instead of stopping the load generator once it has emitted `length` values
 */
private[core] sealed abstract class Counter(
    key: String,
    first: Int,
    increment: Int,
    length: Long,
    wrapAround: Boolean,
    controller: ActorRef[Controller.Command],
    override val statsEngine: StatsEngine,
    override val next: Action
) extends ChainableAction
    with NameGen {
  override val name: String = genName("counter")

  protected def emit(index: Long, session: Session): Unit =
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
