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

import io.gatling.commons.stats.KO
import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, Session, TryMaxBlock }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

class TryMax(
    times: Expression[Int],
    counterName: String,
    statsEngine: StatsEngine,
    clock: Clock,
    next: Action
) extends Action
    with NameGen {

  override val name: String = genName("tryMax")

  private[this] var innerTryMax: Action = _
  private[core] def initialize(loopNext: Action): Unit =
    innerTryMax = new InnerTryMax(times, loopNext, counterName, name + "-inner", next)

  override def execute(session: Session): Unit =
    BlockExit.mustExit(session) match {
      case Some(blockExit) => blockExit.exitBlock(statsEngine, clock.nowMillis)
      case _               => innerTryMax ! session
    }
}

class InnerTryMax(
    times: Expression[Int],
    loopNext: Action,
    counterName: String,
    val name: String,
    val next: Action
) extends ChainableAction {

  private[this] val lastUserIdThreadLocal = new ThreadLocal[Long]

  private[this] def getAndSetLastUserId(session: Session): Long = {
    val lastUserId = lastUserIdThreadLocal.get()
    lastUserIdThreadLocal.set(session.userId)
    lastUserId
  }

  private def blockFailed(session: Session): Boolean = session.blockStack.headOption match {
    case Some(TryMaxBlock(_, _, KO)) => true
    case _                           => false
  }

  private def maxNotReached(session: Session): Boolean = {
    val validationResult = for {
      counter <- session(counterName).validate[Int]
      max <- times(session)
    } yield counter < max

    validationResult match {
      case Success(maxNotReached) => maxNotReached
      case Failure(message) =>
        logger.error(s"Condition evaluation for tryMax $counterName crashed with message '$message', exiting tryMax")
        false
    }
  }

  private def continue(session: Session): Boolean = blockFailed(session) && maxNotReached(session)

  /**
   * Evaluates the condition and if true executes the first action of loopNext
   * else it executes next
   *
   * @param session the session of the virtual user
   */
  def execute(session: Session): Unit = {

    val lastUserId = getAndSetLastUserId(session)

    if (!session.contains(counterName)) {
      loopNext ! session.enterTryMax(counterName, this)
    } else {
      val incrementedSession = session.incrementCounter(counterName)

      if (continue(incrementedSession)) {

        // reset status
        val resetSession = incrementedSession.markAsSucceeded

        if (session.userId == lastUserId) {
          // except if we're running only one user per core, it's very likely we're hitting an empty loop
          // let's dispatch so we don't spin
          val eventLoop = session.eventLoop
          if (!eventLoop.isShutdown) {
            eventLoop.execute(() => loopNext ! resetSession)
          }

        } else {
          loopNext ! resetSession
        }

      } else {
        next ! session.exitTryMax
      }
    }
  }
}
