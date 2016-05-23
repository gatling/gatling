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

import io.gatling.core.session.{ Expression, LoopBlock, Session }
import io.gatling.core.stats.StatsEngine

/**
 * Action in charge of controlling a while loop execution.
 *
 * @constructor creates a Loop in the scenario
 * @param continueCondition the condition that decides when to exit the loop
 * @param counterName the name of the counter for this loop
 * @param exitASAP if loop condition should be evaluated between chain elements to exit ASAP
 * @param timeBased if loop is based on time and should compute entry timestamp
 * @param statsEngine the StatsEngine
 * @param next the chain executed if testFunction evaluates to false
 */
class Loop(continueCondition: Expression[Boolean], counterName: String, exitASAP: Boolean, timeBased: Boolean, statsEngine: StatsEngine, val name: String, next: Action) extends Action {

  private[this] var innerLoop: Action = _

  private[core] def initialize(loopNext: Action): Unit = {

    val counterIncrement = (session: Session) =>
      if (!session.contains(counterName))
        session.enterLoop(counterName, continueCondition, this, exitASAP, timeBased)
      else
        session.incrementCounter(counterName)

    innerLoop = new InnerLoop(continueCondition, loopNext, counterIncrement, name + "-inner", next)
  }

  override def execute(session: Session): Unit =
    ExitableAction.exitOrElse(session, statsEngine)(innerLoop.!)
}

class InnerLoop(
    continueCondition: Expression[Boolean],
    loopNext:          Action,
    counterIncrement:  Session => Session,
    val name:          String,
    val next:          Action
) extends ChainableAction {

  /**
   * Evaluates the condition and if true executes the first action of loopNext
   * else it executes next
   *
   * @param session the session of the virtual user
   */
  def execute(session: Session): Unit = {

    val incrementedSession = counterIncrement(session)

    if (LoopBlock.continue(continueCondition, incrementedSession))
      // TODO maybe find a way not to reevaluate in case of exitASAP
      loopNext ! incrementedSession
    else
      next ! incrementedSession.exitLoop
  }
}
