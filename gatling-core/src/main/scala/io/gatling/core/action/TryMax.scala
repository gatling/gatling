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
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.validation.{ Failure, Success }

class TryMax(times: Int, counterName: String, next: ActorRef) extends Actor {

  var innerTryMax: ActorRef = _

  val initialized: Receive = Interruptable.interrupt orElse { case m => innerTryMax forward m }

  val uninitialized: Receive = {
    case loopNext: ActorRef =>
      innerTryMax = actor(new InnerTryMax(times, loopNext, counterName, next))
      context.become(initialized)
  }

  override def receive = uninitialized
}

class InnerTryMax(times: Int, loopNext: ActorRef, counterName: String, val next: ActorRef) extends Chainable with DataWriterClient {

  private def continue(session: Session): Boolean = session(counterName).validate[Int].map(_ < times) match {
    case Success(eval) => eval
    case Failure(message) =>
      logger.error(s"Condition evaluation for tryMax $counterName crashed with message '$message', exiting tryMax")
      false
  }

  /**
   * Evaluates the condition and if true executes the first action of loopNext
   * else it executes next
   *
   * @param session the session of the virtual user
   */
  def execute(session: Session) {

    if (!session.contains(counterName))
      loopNext ! session.enterTryMax(counterName, self)

    else {
      val incrementedSession = session.incrementCounter(counterName)

      if (continue(incrementedSession))
        // reset status
        loopNext ! incrementedSession.markAsSucceeded
      else
        next ! session.exitTryMax
    }
  }
}
