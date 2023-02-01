/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.controller.ControllerCommand
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine

import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging

/**
 * Top level abstraction in charge of executing concrete actions along a scenario, for example sending an HTTP request.
 */
trait Action extends StrictLogging {
  def name: String

  override def toString: String = name

  def !(session: Session): Unit = {
    val eventLoop = session.eventLoop
    if (eventLoop.inEventLoop) {
      execute(session)
    } else if (!eventLoop.isShutdown) {
      eventLoop.execute(() => execute(session))
    }
  }

  /**
   * Core method executed when the Action received a Session message
   *
   * @param session
   *   the session of the virtual user
   * @return
   *   Nothing
   */
  protected def execute(session: Session): Unit
}

/**
 * An Action that is to be chained with another. Almost all Gatling Actions are Chainable. For example, the final Action at the end of a scenario workflow is
 * not.
 */
trait ChainableAction extends Action {

  /**
   * @return
   *   the next Action in the scenario workflow
   */
  def next: Action

  override abstract def !(session: Session): Unit =
    try {
      super.!(session)
    } catch {
      case reason: IllegalStateException if reason.getMessage == "cannot enqueue after timer shutdown" =>
        logger.debug(s"'$name' crashed with '${reason.detailedMessage}', ignoring")
      case NonFatal(reason) =>
        if (logger.underlying.isInfoEnabled) {
          logger.error(s"'$name' crashed on session $session, forwarding to the next one", reason)
        } else {
          logger.error(s"'$name' crashed with '${reason.detailedMessage}', forwarding to the next one")
        }
        next ! session.markAsFailed
      case fatal: Throwable =>
        logger.error(s"Gatling crashed: ${fatal.detailedMessage}", fatal)
        sys.exit(1)
    }

  def recover(session: Session)(v: Validation[_]): Unit =
    v.onFailure { message =>
      logger.error(s"'$name' failed to execute: $message")
      next ! session.markAsFailed
    }
}

class ActorDelegatingAction(val name: String, actor: ActorRef) extends Action {
  def execute(session: Session): Unit = actor ! session
}

/**
 * An Action that can trigger a forced exit and bypass regular workflow.
 */
trait ExitableAction extends ChainableAction {
  def statsEngine: StatsEngine
  def clock: Clock

  override abstract def !(session: Session): Unit =
    BlockExit.mustExit(session) match {
      case Some(blockExit) => blockExit.exitBlock(statsEngine, clock.nowMillis)
      case _               => super.!(session)
    }
}

trait RequestAction extends ExitableAction {
  def requestName: Expression[String]
  def sendRequest(session: Session): Validation[Unit]

  private def reportUnbuildableRequest(session: Session, error: String): Unit = {
    val loggedName = requestName(session) match {
      case Success(requestNameValue) =>
        statsEngine.reportUnbuildableRequest(session.scenario, session.groups, error, requestNameValue)
        requestNameValue
      case _ =>
        name
    }
    logger.error(s"'$loggedName' failed to execute: $error")
  }

  override def execute(session: Session): Unit =
    try {
      sendRequest(session) match {
        case Failure(error) =>
          reportUnbuildableRequest(session, error)
          next ! session.markAsFailed
        case _ =>
      }
    } catch {
      case NonFatal(e) =>
        reportUnbuildableRequest(session, e.detailedMessage)
        // rethrow so we trigger exception handling in "ChainableAction!"
        throw e
    }
}
