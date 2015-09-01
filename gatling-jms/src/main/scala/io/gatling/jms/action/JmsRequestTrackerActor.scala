/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

import javax.jms.Message

import scala.collection.mutable

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.validation.Failure
import io.gatling.core.Predef.Session
import io.gatling.core.akka.BaseActor
import io.gatling.core.check.Check
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.jms._

import akka.actor.{ ActorRef, Props }

/**
 * Advise actor a message was sent to JMS provider
 */
case class MessageSent(
  requestId: String,
  startDate: Long,
  checks:    List[JmsCheck],
  session:   Session,
  next:      ActorRef,
  title:     String
)

/**
 * Advise actor a response message was received from JMS provider
 */
case class MessageReceived(responseId: String, received: Long, message: Message)

object JmsRequestTrackerActor {
  def props(statsEngine: StatsEngine) = Props(new JmsRequestTrackerActor(statsEngine))
}

/**
 * Bookkeeping actor to correlate request and response JMS messages
 * Once a message is correlated, it publishes to the Gatling core DataWriter
 */
class JmsRequestTrackerActor(statsEngine: StatsEngine) extends BaseActor {

  // messages to be tracked through this HashMap - note it is a mutable hashmap
  val sentMessages = mutable.HashMap.empty[String, (Long, List[JmsCheck], Session, ActorRef, String)]
  val receivedMessages = mutable.HashMap.empty[String, (Long, Message)]

  // Actor receive loop
  def receive = {

    // message was sent; add the timestamps to the map
    case MessageSent(corrId, startDate, checks, session, next, title) =>
      receivedMessages.get(corrId) match {
        case Some((receivedDate, message)) =>
          // message was received out of order, lets just deal with it
          processMessage(session, startDate, receivedDate, checks, message, next, title)
          receivedMessages -= corrId

        case None =>
          // normal path
          val sentMessage = (startDate, checks, session, next, title)
          sentMessages += corrId -> sentMessage
      }

    // message was received; publish to the datawriter and remove from the hashmap
    case MessageReceived(corrId, receivedDate, message) =>
      sentMessages.get(corrId) match {
        case Some((startDate, checks, session, next, title)) =>
          processMessage(session, startDate, receivedDate, checks, message, next, title)
          sentMessages -= corrId

        case None =>
          // failed to find message; early receive? or bad return correlation id?
          // let's add it to the received messages buffer just in case
          val receivedMessage = (receivedDate, message)
          receivedMessages += corrId -> receivedMessage
      }
  }

  /**
   * Processes a matched message
   */
  def processMessage(
    session:      Session,
    startDate:    Long,
    receivedDate: Long,
    checks:       List[JmsCheck],
    message:      Message,
    next:         ActorRef,
    title:        String
  ): Unit = {

      def executeNext(updatedSession: Session, status: Status, message: Option[String] = None) = {
        val timings = ResponseTimings(startDate, receivedDate)
        statsEngine.logResponse(updatedSession, title, timings, status, None, message)
        next ! updatedSession.logGroupRequest(timings.responseTime, status).increaseDrift(nowMillis - receivedDate)
      }

    // run all the checks, advise the Gatling API that it is complete and move to next
    val (checkSaveUpdate, error) = Check.check(message, session, checks)
    val newSession = checkSaveUpdate(session)
    error match {
      case None                 => executeNext(newSession, OK)
      case Some(Failure(error)) => executeNext(newSession.markAsFailed, KO, Some(error))
    }
  }
}
