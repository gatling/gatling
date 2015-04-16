/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import akka.actor.{ Props, ActorRef }
import io.gatling.core.akka.BaseActor
import io.gatling.core.result.message.KO
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session.{ TryMaxBlock, Session }
import io.gatling.core.validation.{ Failure, Success }

object TryMax {
  def props(times: Int, counterName: String, dataWriters: DataWriters, next: ActorRef) =
    Props(new TryMax(times, counterName, dataWriters, next))
}

class TryMax(times: Int, counterName: String, dataWriters: DataWriters, next: ActorRef) extends BaseActor {

  def initialized(innerTryMax: ActorRef): Receive =
    Interruptable.interrupt(dataWriters) orElse { case m => innerTryMax forward m }

  val uninitialized: Receive = {
    case loopNext: ActorRef =>
      val actorName = self.path.name + "-inner"
      val innerTryMax = context.actorOf(InnerTryMax.props(times, loopNext, counterName, next), actorName)
      context.become(initialized(innerTryMax))
  }

  override def receive = uninitialized
}

object InnerTryMax {
  def props(times: Int, loopNext: ActorRef, counterName: String, next: ActorRef) =
    Props(new InnerTryMax(times, loopNext, counterName, next))
}

class InnerTryMax(times: Int, loopNext: ActorRef, counterName: String, val next: ActorRef)
    extends Chainable {

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
