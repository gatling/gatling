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

import java.util.concurrent.atomic.AtomicBoolean
import javax.jms.Message

import scala.util.Try
import scala.util.control.NonFatal

import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.validation._
import io.gatling.core.action.{ Failable, Interruptable }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.jms.client.JmsClient
import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request._

import akka.actor.{ ActorRef, Props }

object JmsReqReplyAction {
  val BlockingReceiveReturnedNull = new Exception("Blocking receive returned null. Possibly the consumer was closed.")

  def props(attributes: JmsAttributes, protocol: JmsProtocol, tracker: ActorRef, statsEngine: StatsEngine, next: ActorRef) =
    Props(new JmsReqReplyAction(attributes, protocol, tracker, statsEngine, next))
}

/**
 * Core JMS Action to handle Request-Reply semantics
 * <p>
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send.
 * This implementation then forwards it on to a tracking actor.
 */
class JmsReqReplyAction(attributes: JmsAttributes, protocol: JmsProtocol, tracker: ActorRef, val statsEngine: StatsEngine, val next: ActorRef)
    extends Interruptable with Failable {

  import JmsReqReplyAction._

  // Create a client to refer to
  val client = JmsClient(protocol, attributes.destination, attributes.replyDestination)

  val receiveTimeout = protocol.receiveTimeout.getOrElse(0L)
  val messageMatcher = protocol.messageMatcher

  class ListenerThread(val continue: AtomicBoolean = new AtomicBoolean(true)) extends Thread(new Runnable {
    def run(): Unit = {
      val replyConsumer = client.createReplyConsumer(attributes.selector.orNull)
      try {
        while (continue.get) {
          val m = replyConsumer.receive(receiveTimeout)
          m match {
            case msg: Message =>
              tracker ! MessageReceived(messageMatcher.responseID(msg), nowMillis, msg)
              logMessage(s"Message received ${msg.getJMSMessageID}", msg)
            case _ =>
              throw BlockingReceiveReturnedNull
          }
        }
      } catch {
        // when we close, receive can throw exception
        case NonFatal(e) => logger.error(e.getMessage)
      } finally {
        replyConsumer.close()
      }
    }
  }) {
    def close() = {
      continue.set(false)
      interrupt()
      join(1000)
    }
  }

  val listenerThreads = (1 to protocol.listenerCount).map(_ => new ListenerThread)

  listenerThreads.foreach(_.start)

  override def postStop(): Unit = {
    listenerThreads.foreach(thread => Try(thread.close()).recover { case NonFatal(e) => logger.warn("Could not shutdown listener thread", e) })
    client.close()
  }

  /**
   * Framework calls the execute() method to send a single request
   * <p>
   * Note this does not catch any exceptions (even JMSException) as generally these indicate a
   * configuration failure that is unlikely to be addressed by retrying with another message
   */
  def executeOrFail(session: Session): Validation[Unit] = {

    val messageProperties = resolveProperties(attributes.messageProperties, session)

    // send the message
    val startDate = nowMillis

    val msg = messageProperties.flatMap { props =>
      attributes.message match {
        case BytesJmsMessage(bytes) => bytes(session).map(bytes => client.sendBytesMessage(bytes, props))
        case MapJmsMessage(map)     => map(session).map(map => client.sendMapMessage(map, props))
        case ObjectJmsMessage(o)    => o(session).map(o => client.sendObjectMessage(o, props))
        case TextJmsMessage(txt)    => txt(session).map(txt => client.sendTextMessage(txt, props))
      }
    }

    msg.map { msg =>
      // notify the tracker that a message was sent
      tracker ! MessageSent(messageMatcher.requestID(msg), startDate, attributes.checks, session, next, attributes.requestName)
      logMessage(s"Message sent ${msg.getJMSMessageID}", msg)
    }
  }

  def resolveProperties(
    properties: Map[Expression[String], Expression[Any]],
    session:    Session
  ): Validation[Map[String, Any]] = {
    properties.foldLeft(Map.empty[String, Any].success) {
      case (resolvedProperties, (key, value)) =>
        val newProperty: Validation[(String, Any)] =
          for {
            key <- key(session)
            value <- value(session)
          } yield key -> value

        for {
          newProperty <- newProperty
          resolvedProperties <- resolvedProperties
        } yield resolvedProperties + newProperty
    }
  }

  def logMessage(text: String, msg: Message): Unit = {
    logger.debug(text)
    logger.trace(msg.toString)
  }
}
