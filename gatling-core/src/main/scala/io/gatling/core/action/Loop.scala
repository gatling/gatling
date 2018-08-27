/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.Clock
import io.gatling.core.session.{ Expression, LoopBlock, Session }
import io.gatling.core.stats.StatsEngine

import akka.actor.ActorSystem

class Loop(
    continueCondition:          Expression[Boolean],
    counterName:                String,
    exitASAP:                   Boolean,
    timeBased:                  Boolean,
    evaluateConditionAfterLoop: Boolean,
    statsEngine:                StatsEngine,
    clock:                      Clock,
    override val name:          String,
    next:                       Action
) extends Action {

  private[this] var innerLoop: Action = _

  private[core] def initialize(loopNext: Action, actorSystem: ActorSystem): Unit = {

    val counterIncrement = (session: Session) =>
      if (session.contains(counterName)) {
        session.incrementCounter(counterName)
      } else if (timeBased) {
        session.enterTimeBasedLoop(counterName, continueCondition, next, exitASAP, clock.nowMillis)
      } else {
        session.enterLoop(counterName, continueCondition, next, exitASAP)
      }

    innerLoop = new InnerLoop(continueCondition, loopNext, counterIncrement, counterName, evaluateConditionAfterLoop, actorSystem, name + "-inner", next)
  }

  override def execute(session: Session): Unit =
    if (BlockExit.noBlockExitTriggered(session, statsEngine, clock.nowMillis)) {
      innerLoop ! session
    }
}

class InnerLoop(
    continueCondition:          Expression[Boolean],
    loopNext:                   Action,
    counterIncrement:           Session => Session,
    counterName:                String,
    evaluateConditionAfterLoop: Boolean,
    actorSystem:                ActorSystem,
    val name:                   String,
    val next:                   Action
) extends ChainableAction {

  private[this] val lastUserIdThreadLocal = new ThreadLocal[Long]

  private[this] def getAndSetLastUserId(session: Session): Long = {
    val lastUserId = lastUserIdThreadLocal.get()
    lastUserIdThreadLocal.set(session.userId)
    lastUserId
  }

  /**
   * Evaluates the condition and if true executes the first action of loopNext
   * else it executes next
   *
   * @param session the session of the virtual user
   */
  def execute(session: Session): Unit = {
    val incrementedSession = counterIncrement(session)
    val lastUserId = getAndSetLastUserId(session)

    // checking if we don't need to evaluate the condition the first time
    if ((incrementedSession.attributes(counterName) == 0 && evaluateConditionAfterLoop)
      || LoopBlock.continue(continueCondition, incrementedSession)) {

      if (incrementedSession.userId == lastUserId) {
        // except if we're running only one user, it's very likely we're hitting an empty loop
        // let's dispatch so we don't spin
        actorSystem.dispatcher.execute(() => loopNext ! incrementedSession)

      } else {
        loopNext ! incrementedSession
      }

    } else {
      next ! incrementedSession.exitLoop
    }
  }
}
