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

package io.gatling.jms.action

import io.gatling.commons.stats.KO
import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.action._
import io.gatling.core.actor.ActorRef
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.jms.client.{ JmsConnectionPool, JmsTracker }
import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request._

import jakarta.jms.{ Destination, Message }

/**
 * Core JMS Action to handle Request-Reply semantics
 *
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send. This implementation then forwards it on to a tracking actor.
 */
final class RequestReply(
    attributes: JmsAttributes,
    replyDestination: JmsDestination,
    setJmsReplyTo: Boolean,
    trackerDestination: Option[JmsDestination],
    protocol: JmsProtocol,
    jmsConnectionPool: JmsConnectionPool,
    override val statsEngine: StatsEngine,
    override val clock: Clock,
    override val next: Action,
    throttler: Option[ActorRef[Throttler.Command]]
) extends JmsAction(attributes, protocol, jmsConnectionPool, throttler) {
  override val name: String = genName("jmsRequestReply")

  private val jmsReplyDestination = jmsConnection.destination(replyDestination)
  private val messageMatcher = protocol.messageMatcher
  private val replyTimeoutInMs = protocol.replyTimeout.fold(0L)(_.toMillis)
  private val jmsTrackerDestination = trackerDestination.map(dest => jmsConnection.destination(dest)).getOrElse(jmsReplyDestination)

  // the matcher can be user code: turn its exceptions into failures, as requestMatchId can be called after the request was sent and logged
  private def requestMatchId(message: Message): Validation[String] =
    safely(error => s"Failed to get a matchId to track: $error")(messageMatcher.requestMatchId(message).success)

  private def prepareRequest(message: Message, replyDestination: Destination): Validation[String] = {
    if (setJmsReplyTo) {
      message.setJMSReplyTo(replyDestination)
    }

    messageMatcher.prepareRequest(message)
    requestMatchId(message)
  }

  override protected def aroundSend(requestName: String, session: Session, message: Message): Validation[Around] =
    for {
      resolvedReplyDestination <- jmsReplyDestination(session)
      resolvedTrackerDestination <- jmsTrackerDestination(session)
      resolvedSelector <- resolveOptionalExpression(attributes.selector, session)
      // notify the tracker that a message was sent
      matchId <- prepareRequest(message, resolvedReplyDestination)
    } yield {
      val tracker = jmsConnection.tracker(resolvedTrackerDestination, resolvedSelector, protocol.listenerThreadCount, messageMatcher)

      new Around(
        before = () => {
          if (logger.underlying.isDebugEnabled) {
            logMessage(s"Message sent matchId=$matchId", message)
          }

          // [e]
          //
          // [e]

          if (matchId != null) {
            tracker ! JmsTracker.Command
              .MessageSent(matchId, clock.nowMillis, replyTimeoutInMs, attributes.checks, attributes.postChecks, session, next, requestName)
          }
        },
        after = () =>
          if (matchId == null) {
            requestMatchId(message).flatMap(id => if (id != null) id.success else "Failed to get a matchId to track".failure) match {
              case Success(updatedMatchId) =>
                tracker ! JmsTracker.Command
                  .MessageSent(updatedMatchId, clock.nowMillis, replyTimeoutInMs, attributes.checks, attributes.postChecks, session, next, requestName)
              case Failure(error) =>
                val now = clock.nowMillis
                statsEngine.logResponse(session.scenario, session.groups, requestName, now, now, KO, None, Some(error))
                next ! session.markAsFailed
            }
          }
      )
    }
}
