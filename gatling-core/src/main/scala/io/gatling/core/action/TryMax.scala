/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.stats.KO
import io.gatling.commons.validation._
import io.gatling.core.session.{ Session, TryMaxBlock }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

class TryMax(times: Int, counterName: String, statsEngine: StatsEngine, next: Action) extends Action with NameGen {

  override val name = genName("tryMax")

  private[this] var innerTryMax: Action = _
  private[core] def initialize(loopNext: Action): Unit =
    innerTryMax = new InnerTryMax(times, loopNext, counterName, name + "-inner", next)

  override def execute(session: Session): Unit =
    ExitableAction.exitOrElse(session, statsEngine)(innerTryMax.!)
}

class InnerTryMax(times: Int, loopNext: Action, counterName: String, val name: String, val next: Action)
    extends ChainableAction {

  private def blockFailed(session: Session): Boolean = session.blockStack.headOption match {
    case Some(TryMaxBlock(_, _, KO)) => true
    case _                           => false
  }

  private def maxNotReached(session: Session): Boolean = session(counterName).validate[Int] match {
    case Success(i) => i < times
    case Failure(message) =>
      logger.error(s"Condition evaluation for tryMax $counterName crashed with message '$message', exiting tryMax")
      false
  }

  private def continue(session: Session): Boolean = blockFailed(session) && maxNotReached(session)

  /**
   * Evaluates the condition and if true executes the first action of loopNext
   * else it executes next
   *
   * @param session the session of the virtual user
   */
  def execute(session: Session): Unit =
    if (!session.contains(counterName)) {
      loopNext ! session.enterTryMax(counterName, this)
    } else {
      val incrementedSession = session.incrementCounter(counterName)

      if (continue(incrementedSession))
        // reset status
        loopNext ! incrementedSession.markAsSucceeded
      else
        next ! session.exitTryMax
    }
}
