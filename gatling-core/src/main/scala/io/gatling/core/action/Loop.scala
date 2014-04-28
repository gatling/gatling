/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.action

import akka.actor.{ Actor, ActorRef }
import akka.actor.ActorDSL.actor
import io.gatling.core.session.{ LoopBlock, Expression, Session }

/**
 * Action in charge of controlling a while loop execution.
 *
 * @constructor creates a Loop in the scenario
 * @param continueCondition the condition that decides when to exit the loop
 * @param counterName the name of the counter for this loop
 * @param next the chain executed if testFunction evaluates to false
 */
class Loop(continueCondition: Expression[Boolean], counterName: String, exitASAP: Boolean, next: ActorRef) extends Actor {

  var innerLoop: ActorRef = _

  val initialized: Receive = Interruptable.interrupt orElse { case m => innerLoop forward m }

  val uninitialized: Receive = {
    case loopNext: ActorRef =>
      innerLoop = actor(new InnerLoop(continueCondition, loopNext, counterName, exitASAP, next))
      context.become(initialized)
  }

  override def receive = uninitialized
}

class InnerLoop(continueCondition: Expression[Boolean], loopNext: ActorRef, counterName: String, exitASAP: Boolean, val next: ActorRef) extends Chainable {

  /**
   * Evaluates the condition and if true executes the first action of loopNext
   * else it executes next
   *
   * @param session the session of the virtual user
   */
  def execute(session: Session) {

    if (!session.contains(counterName))
      loopNext ! session.enterLoop(counterName, continueCondition, self, exitASAP)

    else {
      val incrementedSession = session.incrementCounter(counterName)

      if (LoopBlock.continue(continueCondition, incrementedSession))
        // TODO maybe find a way not to reevaluate in case
        loopNext ! incrementedSession

      else
        next ! session.exitLoop
    }
  }
}
