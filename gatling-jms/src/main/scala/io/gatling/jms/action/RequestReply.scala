/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.core.action._
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.jms.client.JmsRequestReplyClient
import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request._

import akka.actor.{ ActorRef, ActorSystem }

object RequestReply {
  val BlockingReceiveReturnedNullException = new Exception("Blocking receive returned null. Possibly the consumer was closed.")
}

/**
 * Core JMS Action to handle Request-Reply semantics
 *
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send.
 * This implementation then forwards it on to a tracking actor.
 */
class RequestReply(override val attributes: JmsAttributes, replyDestination: JmsDestination, protocol: JmsProtocol, tracker: ActorRef, system: ActorSystem, val statsEngine: StatsEngine, val next: Action)
    extends JmsAction[JmsRequestReplyClient] with ExitableAction with NameGen {

  override val name = genName("jmsRequestReply")

  override val client = new JmsRequestReplyClient(protocol, attributes.destination, replyDestination)

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
              throw RequestReply.BlockingReceiveReturnedNullException
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

  system.registerOnTermination {
    listenerThreads.foreach(thread => Try(thread.close()).recover { case NonFatal(e) => logger.warn("Could not shutdown listener thread", e) })
    client.close()
  }

  protected override def beforeSend(requestName: String, session: Session)(message: Message): Unit = {
    // notify the tracker that a message was sent
    val matchId = messageMatcher.requestMatchId(message)
    if (logger.underlying.isDebugEnabled()) {
      logMessage(s"Message sent JMSMessageID=${message.getJMSMessageID} matchId=$matchId", message)
    }
    // [fl]
    //
    // [/fl]
    tracker ! MessageSent(replyDestinationName, matchId, nowMillis, attributes.checks, session, next, requestName)
  }
}
