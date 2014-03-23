/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jms

import akka.actor.ActorRef
import io.gatling.core.action.{ Failable, Interruptable }
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import javax.jms.Message
import java.util.concurrent.atomic.AtomicBoolean

object JmsReqReplyAction {

	val blockingReceiveReturnedNull = new Exception("Blocking receive returned null. Possibly the consumer was closed.")
}

/**
 * Core JMS Action to handle Request-Reply semantics
 * <p>
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send.
 * This implementation then forwards it on to a tracking actor.
 * @author jasonk@bluedevel.com
 */
class JmsReqReplyAction(val next: ActorRef, attributes: JmsAttributes, protocol: JmsProtocol, tracker: ActorRef) extends Interruptable with Failable {

	// Create a client to refer to
	val client = new SimpleJmsClient(
		protocol.connectionFactoryName,
		attributes.queueName,
		protocol.url,
		protocol.credentials,
		protocol.contextFactory,
		protocol.deliveryMode)

	class ListenerThread(val continue: AtomicBoolean = new AtomicBoolean(true)) extends Thread(new Runnable {
		def run() = {
			val replyConsumer = client.createReplyConsumer
			while (continue.get) {
				val m = replyConsumer.receive
				m match {
					case msg: Message => tracker ! MessageReceived(msg.getJMSCorrelationID, nowMillis, msg)
					case _ =>
						logger.error(JmsReqReplyAction.blockingReceiveReturnedNull.getMessage)
						throw JmsReqReplyAction.blockingReceiveReturnedNull
				}
			}
		}
	})

	val listenerThreads = (1 to protocol.listenerCount).map(_ => new ListenerThread)

	listenerThreads.foreach(_.start)

	override def postStop() {
		client.close()
		listenerThreads.foreach(_.continue.set(false))
	}

	/**
	 * Framework calls the execute() method to send a single request
	 * <p>
	 * Note this does not catch any exceptions (even JMSException) as generally these indicate a
	 * configuration failure that is unlikely to be addressed by retrying with another message
	 */
	def executeOrFail(session: Session) = {

		// send the message
		val start = nowMillis

		val msgid = attributes.message match {
			case BytesJmsMessage(bytes) => bytes(session).map(bytes => client.sendBytesMessage(bytes, attributes.messageProperties))
			case MapJmsMessage(map) => map(session).map(map => client.sendMapMessage(map, attributes.messageProperties))
			case ObjectJmsMessage(o) => o(session).map(o => client.sendObjectMessage(o, attributes.messageProperties))
			case TextJmsMessage(txt) => txt(session).map(txt => client.sendTextMessage(txt, attributes.messageProperties))
		}

		msgid.map { msgid =>
			// notify the tracker that a message was sent
			tracker ! MessageSent(msgid, start, nowMillis, attributes.checks, session, next, attributes.requestName)
		}
	}
}
