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

import akka.actor.ActorRef
import io.gatling.core.akka.BaseActor
import io.gatling.core.validation.Validation
import io.gatling.core.session.Session

/**
 * Top level abstraction in charge of executing concrete actions along a scenario, for example sending an HTTP request.
 * It is implemented as an Akka Actor that receives Session messages.
 */
trait Action extends BaseActor {

  def receive = {
    case session: Session => execute(session)
  }

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
trait Chainable extends Action {

  /**
   * @return the next Action in the scenario workflow
   */
  def next: ActorRef

  /**
   * Makes sure that in case of an actor crash, the Session is not lost but passed to the next Action.
   */
  override def preRestart(reason: Throwable, message: Option[Any]) {
    message.foreach {
      case session: Session =>
        logger.error(s"Action $this crashed on session $message, forwarding to the next one", reason)
        next ! session.markAsFailed
      case _ =>
        logger.error(s"Action $this crashed on unknown message $message, dropping", reason)
    }
  }
}

/**
 * An Action that handles failures gracefully by returning a Validation
 */
trait Failable { self: Chainable =>

  def execute(session: Session) {
    executeOrFail(session).onFailure { message =>
      logger.error(message)
      next ! session.markAsFailed
    }
  }

  def executeOrFail(session: Session): Validation[_]
}
