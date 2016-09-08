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
package io.gatling.jms.action

import java.lang.{ Boolean => JBoolean }
import java.util.{ Collections => JCollections, LinkedHashMap => JLinkedHashMap, Map => JMap }
import javax.jms.Message

import scala.collection.mutable

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.validation.Failure
import io.gatling.core.action.Action
import io.gatling.core.akka.BaseActor
import io.gatling.core.check.Check
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.jms._

import akka.actor.Props

/**
 * Advise actor a message was sent to JMS provider
 */
case class MessageSent(
  replyDestinationName: String,
  matchId:              String,
  sent:                 Long,
  checks:               List[JmsCheck],
  session:              Session,
  next:                 Action,
  title:                String
)

/**
 * Advise actor a response message was received from JMS provider
 */
case class MessageReceived(
  replyDestinationName: String,
  matchId:              String,
  received:             Long,
  message:              Message
)

/**
 * Advise actor that the blocking receive failed
 */
case object BlockingReceiveReturnedNull

object JmsRequestTrackerActor {
  def props(statsEngine: StatsEngine, configuration: GatlingConfiguration) = Props(new JmsRequestTrackerActor(statsEngine, configuration))
}

case class MessageKey(replyDestinationName: String, matchId: String)

/**
 * Bookkeeping actor to correlate request and response JMS messages
 * Once a message is correlated, it publishes to the Gatling core DataWriter
 */
class JmsRequestTrackerActor(statsEngine: StatsEngine, configuration: GatlingConfiguration) extends BaseActor {

  private val sentMessages = mutable.HashMap.empty[MessageKey, MessageSent]
  private val receivedMessages = mutable.HashMap.empty[MessageKey, MessageReceived]
  private val duplicateMessageProtectionEnabled = configuration.jms.acknowledgedMessagesBufferSize > 0
  private val acknowledgedMessagesHistory: JMap[MessageKey, JBoolean] =
    if (duplicateMessageProtectionEnabled)
      new JLinkedHashMap[MessageKey, JBoolean](configuration.jms.acknowledgedMessagesBufferSize, 0.75f, false)
    else
      JCollections.emptyMap()

  // Actor receive loop
  def receive = {

    // message was sent; add the timestamps to the map
    case messageSent @ MessageSent(queueName, correlationId, sent, checks, session, next, title) =>
      val messageKey = MessageKey(queueName, correlationId)
      receivedMessages.get(messageKey) match {
        case None =>
          // normal path
          sentMessages += messageKey -> messageSent

        case Some(MessageReceived(_, _, received, message)) =>
          // message was received out of order, lets just deal with it
          processMessage(session, sent, received, checks, message, next, title)
          receivedMessages -= messageKey
      }

    // message was received; publish to the datawriter and remove from the hashmap
    case messageReceived @ MessageReceived(queueName, correlationId, received, message) =>
      val messageKey = MessageKey(queueName, correlationId)
      sentMessages.get(messageKey) match {
        case Some(MessageSent(_, _, sent, checks, session, next, title)) =>
          processMessage(session, sent, received, checks, message, next, title)
          sentMessages -= messageKey
          if (duplicateMessageProtectionEnabled) {
            acknowledgedMessagesHistory.put(messageKey, JBoolean.TRUE)
          }

        case None =>
          if (!acknowledgedMessagesHistory.containsValue(messageKey)) {
            // failed to find message; early receive? or bad return correlation id?
            // let's add it to the received messages buffer just in case
            receivedMessages += messageKey -> messageReceived
          }
        // else, message was already acked and is a dup
      }

    case BlockingReceiveReturnedNull =>
      //Fail all the sent messages because we do not even have a correlation id
      sentMessages.foreach {
        case (messageKey, MessageSent(_, _, sent, checks, session, next, title)) =>
          executeNext(session, sent, nowMillis, KO, next, title, Some("Blocking received returned null"))
          sentMessages -= messageKey
      }
  }

  private def executeNext(
    session:  Session,
    sent:     Long,
    received: Long,
    status:   Status,
    next:     Action,
    title:    String,
    message:  Option[String] = None
  ) = {
    val timings = ResponseTimings(sent, received)
    statsEngine.logResponse(session, title, timings, status, None, message)
    next ! session.logGroupRequest(timings.responseTime, status).increaseDrift(nowMillis - received)
  }

  /**
   * Processes a matched message
   */
  private def processMessage(
    session:  Session,
    sent:     Long,
    received: Long,
    checks:   List[JmsCheck],
    message:  Message,
    next:     Action,
    title:    String
  ): Unit = {
    // run all the checks, advise the Gatling API that it is complete and move to next
    val (checkSaveUpdate, error) = Check.check(message, session, checks)
    val newSession = checkSaveUpdate(session)
    error match {
      case None                        => executeNext(newSession, sent, received, OK, next, title)
      case Some(Failure(errorMessage)) => executeNext(newSession.markAsFailed, sent, received, KO, next, title, Some(errorMessage))
    }
  }
}
