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

import scala.util.control.NonFatal

import io.gatling.commons.validation.Validation
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine

import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging

/**
 * Top level abstraction in charge of executing concrete actions along a scenario, for example sending an HTTP request.
 * It is implemented as an Akka Actor that receives Session messages.
 */
trait Action extends StrictLogging {

  def name: String

  def !(session: Session): Unit = execute(session)

  /**
   * Core method executed when the Action received a Session message
   *
   * @param session the session of the virtual user
   * @return Nothing
   */
  def execute(session: Session): Unit
}

/**
 * An Action that is to be chained with another.
 * Almost all Gatling Actions are Chainable.
 * For example, the final Action at the end of a scenario workflow is not.
 */
trait ChainableAction extends Action {

  /**
   * @return the next Action in the scenario workflow
   */
  def next: Action

  abstract override def !(session: Session): Unit =
    try {
      super.!(session)
    } catch {
      case NonFatal(reason) =>
        if (logger.underlying.isInfoEnabled)
          logger.error(s"'$name' crashed on session $session, forwarding to the next one", reason)
        else if (reason.getMessage == null)
          logger.error(s"'$name' crashed with '${reason.getClass.getName}', forwarding to the next one")
        else
          logger.error(s"'$name' crashed with '${reason.getClass.getName}: ${reason.getMessage}', forwarding to the next one")
        next.execute(session.markAsFailed)
    }

  def recover(session: Session)(v: Validation[_]): Unit =
    v.onFailure { message =>
      logger.error(s"'$name' failed to execute: $message")
      next.execute(session.markAsFailed)
    }
}

class ActorDelegatingAction(val name: String, actor: ActorRef) extends Action {

  def execute(session: Session): Unit = actor ! session
}

class ExitableActorDelegatingAction(name: String, val statsEngine: StatsEngine, val next: Action, actor: ActorRef) extends ActorDelegatingAction(name, actor) with ExitableAction
