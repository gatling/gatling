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
package io.gatling.jms

import scala.collection.mutable
import akka.actor.{ Props, ActorRef }

import io.gatling.core.Predef.Session
import io.gatling.core.akka.BaseActor
import io.gatling.core.result.message.{ ResponseTimings, Status, KO, OK }
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.validation.Failure

import javax.jms.Message
import io.gatling.core.check.Check
import io.gatling.core.util.TimeHelper.nowMillis

/**
 * Advise actor a message was sent to JMS provider
 * @author jasonk@bluedevel.com
 */
case class MessageSent(
  requestId: String,
  startSend: Long,
  endSend: Long,
  checks: List[JmsCheck],
  session: Session,
  next: ActorRef,
  title: String)

/**
 * Advise actor a response message was received from JMS provider
 */
case class MessageReceived(responseId: String, received: Long, message: Message)

object JmsRequestTrackerActor {
  def props(dataWriters: DataWriters) = Props(new JmsRequestTrackerActor(dataWriters))
}

/**
 * Bookkeeping actor to correlate request and response JMS messages
 * Once a message is correlated, it publishes to the Gatling core DataWriter
 */
class JmsRequestTrackerActor(dataWriters: DataWriters) extends BaseActor {

  // messages to be tracked through this HashMap - note it is a mutable hashmap
  val sentMessages = mutable.HashMap.empty[String, (Long, Long, List[JmsCheck], Session, ActorRef, String)]
  val receivedMessages = mutable.HashMap.empty[String, (Long, Message)]

  // Actor receive loop
  def receive = {

    // message was sent; add the timestamps to the map
    case MessageSent(corrId, startSend, endSend, checks, session, next, title) =>
      receivedMessages.get(corrId) match {
        case Some((received, message)) =>
          // message was received out of order, lets just deal with it
          processMessage(session, startSend, received, endSend, checks, message, next, title)
          receivedMessages -= corrId

        case None =>
          // normal path
          val sentMessage = (startSend, endSend, checks, session, next, title)
          sentMessages += corrId -> sentMessage
      }

    // message was received; publish to the datawriter and remove from the hashmap
    case MessageReceived(corrId, received, message) =>
      sentMessages.get(corrId) match {
        case Some((startSend, endSend, checks, session, next, title)) =>
          processMessage(session, startSend, received, endSend, checks, message, next, title)
          sentMessages -= corrId

        case None =>
          // failed to find message; early receive? or bad return correlation id?
          // let's add it to the received messages buffer just in case
          val receivedMessage = (received, message)
          receivedMessages += corrId -> receivedMessage
      }
  }

  /**
   * Processes a matched message
   */
  def processMessage(session: Session,
                     startSend: Long,
                     received: Long,
                     endSend: Long,
                     checks: List[JmsCheck],
                     message: Message,
                     next: ActorRef,
                     title: String): Unit = {

      def executeNext(updatedSession: Session, status: Status, message: Option[String] = None) = {
        val timings = ResponseTimings(startSend, endSend, endSend, received)
        dataWriters.logResponse(updatedSession, title, timings, status, None, message)
        next ! updatedSession.logGroupRequest((received - startSend).toInt, status).increaseDrift(nowMillis - received)
      }

    // run all the checks, advise the Gatling API that it is complete and move to next
    val (checkSaveUpdate, error) = Check.check(message, session, checks)
    val newSession = checkSaveUpdate(session)
    error match {
      case None                   => executeNext(newSession, OK)
      case Some(Failure(message)) => executeNext(newSession.markAsFailed, KO, Some(message))
    }
  }
}
