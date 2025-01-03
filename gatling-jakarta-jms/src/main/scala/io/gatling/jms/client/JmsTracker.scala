/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.jms.client

import scala.collection.mutable
import scala.concurrent.duration._

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.Clock
import io.gatling.commons.validation.Failure
import io.gatling.core.action.Action
import io.gatling.core.actor.{ Actor, Behavior }
import io.gatling.core.check.Check
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.jms._

import jakarta.jms.Message

object JmsTracker {
  def actor(actorName: String, statsEngine: StatsEngine, clock: Clock, configuration: GatlingConfiguration): Actor[Command] =
    new JmsTracker(actorName, statsEngine, clock, configuration.jms.replyTimeoutScanPeriod)

  sealed trait Command

  object Command {
    final case class MessageSent(
        matchId: String,
        sent: Long,
        replyTimeoutInMs: Long,
        checks: List[JmsCheck],
        session: Session,
        next: Action,
        requestName: String
    ) extends Command

    final case class MessageReceived(
        matchId: String,
        received: Long,
        message: Message
    ) extends Command

    case object TimeoutScan extends Command
  }
}

/**
 * Bookkeeping actor to correlate request and response JMS messages Once a message is correlated, it publishes to the Gatling core DataWriter
 */
private final class JmsTracker private (actorName: String, statsEngine: StatsEngine, clock: Clock, replyTimeoutScanPeriod: FiniteDuration)
    extends Actor[JmsTracker.Command](actorName) {

  import JmsTracker.Command._

  private val sentMessages = mutable.HashMap.empty[String, MessageSent]
  private val timedOutMessages = mutable.ArrayBuffer.empty[MessageSent]
  private var periodicTimeoutScanTriggered = false

  private def triggerPeriodicTimeoutScan(): Unit =
    if (!periodicTimeoutScanTriggered) {
      periodicTimeoutScanTriggered = true
      scheduler.scheduleAtFixedRate(replyTimeoutScanPeriod) {
        self ! TimeoutScan
      }
    }

  override def init(): Behavior[JmsTracker.Command] = {
    // message was sent; add the timestamps to the map
    case messageSent: MessageSent =>
      sentMessages += messageSent.matchId -> messageSent
      if (messageSent.replyTimeoutInMs > 0) {
        triggerPeriodicTimeoutScan()
      }
      stay

    // message was received; publish stats and remove from the hashmap
    case MessageReceived(matchId, received, message) =>
      // if key is missing, message was already acked and is a dup, or request timedout
      sentMessages.remove(matchId).foreach { case MessageSent(_, sent, _, checks, session, next, requestName) =>
        processMessage(session, sent, received, checks, message, next, requestName)
      }
      stay

    case TimeoutScan =>
      val now = clock.nowMillis
      sentMessages.valuesIterator.foreach { message =>
        val replyTimeoutInMs = message.replyTimeoutInMs
        if (replyTimeoutInMs > 0 && (now - message.sent) > replyTimeoutInMs) {
          timedOutMessages += message
        }
      }

      for (MessageSent(matchId, sent, replyTimeoutInMs, _, session, next, requestName) <- timedOutMessages) {
        sentMessages.remove(matchId)
        executeNext(session.markAsFailed, sent, now, KO, next, requestName, Some(s"Reply timeout after $replyTimeoutInMs ms"))
      }
      timedOutMessages.clear()
      stay
  }

  private def executeNext(
      session: Session,
      sent: Long,
      received: Long,
      status: Status,
      next: Action,
      requestName: String,
      message: Option[String]
  ): Unit = {
    statsEngine.logResponse(session.scenario, session.groups, requestName, sent, received, status, None, message)
    next ! session.logGroupRequestTimings(sent, received)
  }

  /**
   * Processes a matched message
   */
  private def processMessage(
      session: Session,
      sent: Long,
      received: Long,
      checks: List[JmsCheck],
      message: Message,
      next: Action,
      requestName: String
  ): Unit = {
    // run all the checks, advise the Gatling API that it is complete and move to next
    val (newSession, error) = Check.check(CachingMessage(message), session, checks)
    error match {
      case Some(Failure(errorMessage)) => executeNext(newSession.markAsFailed, sent, received, KO, next, requestName, Some(errorMessage))
      case _                           => executeNext(newSession, sent, received, OK, next, requestName, None)
    }
  }
}
