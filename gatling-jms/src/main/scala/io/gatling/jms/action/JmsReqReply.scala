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

import java.util.concurrent.atomic.AtomicBoolean
import javax.jms.Message

import scala.util.Try
import scala.util.control.NonFatal

import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.validation._
import io.gatling.core.action._
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.jms.client.{ JmsClient, JmsReqReplyClient }
import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request._

import akka.actor.{ ActorRef, ActorSystem, Props }

/**
 * Core JMS Action to handle Request-Reply semantics
 *
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send.
 * This implementation then forwards it on to a tracking actor.
 */
object JmsReqReply extends NameGen {

  def apply(attributes: JmsAttributes, replyDestination: JmsDestination, protocol: JmsProtocol, tracker: ActorRef, system: ActorSystem, statsEngine: StatsEngine, next: Action) = {
    val actor = system.actorOf(JmsReqReplyActor.props(attributes, replyDestination, protocol, tracker, statsEngine, next))
    new ExitableActorDelegatingAction(genName("jmsReqReply"), statsEngine, next, actor)
  }
}

object JmsReqReplyActor {
  val BlockingReceiveReturnedNullException = new Exception("Blocking receive returned null. Possibly the consumer was closed.")

  def props(attributes: JmsAttributes, replyDestination: JmsDestination, protocol: JmsProtocol, tracker: ActorRef, statsEngine: StatsEngine, next: Action) =
    Props(new JmsReqReplyActor(attributes, replyDestination, protocol, tracker, statsEngine, next))
}

class JmsReqReplyActor(override val attributes: JmsAttributes, replyDestination: JmsDestination, protocol: JmsProtocol, tracker: ActorRef, val statsEngine: StatsEngine, val next: Action)
    extends ValidatedActionActor with JmsAction[JmsReqReplyClient] {

  // Create a client to refer to
  override val client = JmsClient(protocol, attributes.destination, replyDestination)

  val receiveTimeout = protocol.receiveTimeout.getOrElse(0L)
  val messageMatcher = protocol.messageMatcher
  val replyDestinationName = client.replyDestinationName

  class ListenerThread(val continue: AtomicBoolean = new AtomicBoolean(true)) extends Thread(new Runnable {
    def run(): Unit = {
      val replyConsumer = client.createReplyConsumer(attributes.selector.orNull)
      try {
        while (continue.get) {
          val m = replyConsumer.receive(receiveTimeout)
          m match {
            case msg: Message =>
              val matchId = messageMatcher.responseMatchId(msg)
              logMessage(s"Message received JMSMessageID=${msg.getJMSMessageID} matchId=$matchId", msg)
              tracker ! MessageReceived(replyDestinationName, matchId, nowMillis, msg)
            case _ =>
              tracker ! BlockingReceiveReturnedNull
              throw JmsReqReplyActor.BlockingReceiveReturnedNullException
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
  def executeOrFail(session: Session): Validation[Unit] =
    sendMessage(session) {
      case (msg, startDate) =>
        // notify the tracker that a message was sent
        val matchId = messageMatcher.requestMatchId(msg)
        if (logger.underlying.isDebugEnabled()) {
          logMessage(s"Message sent JMSMessageID=${msg.getJMSMessageID} matchId=$matchId", msg)
        }
        tracker ! MessageSent(replyDestinationName, matchId, startDate, attributes.checks, session, next, attributes.requestName)
    }
}
