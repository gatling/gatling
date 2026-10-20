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

import scala.concurrent.duration.FiniteDuration
import scala.runtime.LongRef

import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, LoopBlock, Session }
import io.gatling.core.stats.StatsEngine

private final class Loop(
    continueCondition: Expression[Boolean],
    counterName: String,
    exitASAP: Boolean,
    duration: Option[Expression[FiniteDuration]],
    statsEngine: StatsEngine,
    clock: Clock,
    override val name: String,
    next: Action
) extends Action {
  private[this] var innerLoop: Action = _

  // duration is only evaluated once, when entering the loop
  private def resolveDurationMillis(duration: Expression[FiniteDuration], session: Session): Long =
    safely()(duration(session)) match {
      case Success(d)       => d.toMillis
      case Failure(message) =>
        logger.error(s"Duration evaluation crashed with message '$message', exiting loop")
        // negative so the elapsed time is always greater and the time-based condition is always false
        -1L
    }

  private[core] def initialize(loopNext: Action): Unit = {
    val counterIncrement = (session: Session) =>
      if (session.contains(counterName)) {
        session.incrementCounter(counterName)
      } else {
        duration match {
          case Some(d) => session.enterTimeBasedLoop(counterName, continueCondition, next, exitASAP, clock.nowMillis, resolveDurationMillis(d, session))
          case _       => session.enterLoop(counterName, continueCondition, next, exitASAP)
        }
      }

    innerLoop = new InnerLoop(continueCondition, loopNext, counterIncrement, name + "-inner", statsEngine, next)
  }

  override def execute(session: Session): Unit =
    BlockExit.mustExit(session) match {
      case Some(blockExit) => blockExit.exitBlock(statsEngine, clock.nowMillis)
      case _               => innerLoop ! session
    }
}

final class InnerLoop(
    continueCondition: Expression[Boolean],
    loopNext: Action,
    counterIncrement: Session => Session,
    override val name: String,
    override val statsEngine: StatsEngine,
    override val next: Action
) extends ChainableAction {
  private[this] val lastUserIdThreadLocal = ThreadLocal.withInitial(() => LongRef.zero())

  private[this] def getAndSetLastUserId(session: Session): Long = {
    val lastUserIdRef = lastUserIdThreadLocal.get()
    val prev = lastUserIdRef.elem
    lastUserIdRef.elem = session.userId
    prev
  }

  /**
   * Evaluates the condition and if true executes the first action of loopNext else it executes next
   *
   * @param session
   *   the session of the virtual user
   */
  override def execute(session: Session): Unit = {
    val incrementedSession = counterIncrement(session)
    val lastUserId = getAndSetLastUserId(session)

    if (LoopBlock.continue(continueCondition, incrementedSession)) {
      if (incrementedSession.userId == lastUserId) {
        // except if we're running only one user per core, it's very likely we're hitting an empty loop
        // let's dispatch so we don't spin
        val eventLoop = session.eventLoop
        if (!eventLoop.isShutdown) {
          eventLoop.execute(() => loopNext ! incrementedSession)
        }
      } else {
        loopNext ! incrementedSession
      }
    } else {
      val newSession = incrementedSession.blockStack match {
        case LoopBlock(counterName) :: tail => incrementedSession.exitLoop(counterName, tail)
        case blockStack                     =>
          logger.error(s"Loop $name called exitLoop on Session $incrementedSession but stack $blockStack head isn't a Loop Block, please report.")
          incrementedSession
      }

      next ! newSession
    }
  }
}
