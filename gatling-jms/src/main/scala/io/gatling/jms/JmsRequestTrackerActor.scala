/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.collection.mutable.HashMap

import akka.actor.{ Actor, ActorRef }
import io.gatling.core.Predef.Session
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.writer.{ DataWriter, RequestMessage }
import javax.jms.Message

/**
 * Advise actor a message was sent to JMS provider
 * @author jasonk@bluedevel.com
 */
case class MessageSent(correlationId: String, startSend: Long, endSend: Long,
	checks: List[JmsCheck], session: Session, next: ActorRef, title: String)

/**
 * Advise actor a response message was received from JMS provider
 * @author jasonk@bluedevel.com
 */
case class MessageReceived(correlationId: String, received: Long, message: Message)

/**
 * Bookkeeping actor to correlate request and response JMS messages
 * Once a message is correlated, it publishes to the Gatling core DataWriter
 * @author jasonk@bluedevel.com
 */
class JmsRequestTrackerActor extends Actor {

	// messages to be tracked through this HashMap - note it is a mutable hashmap
	val sentMessages = new HashMap[String, (Long, Long, List[JmsCheck], Session, ActorRef, String)]()
	val receivedMessages = new HashMap[String, (Long, Message)]()

	// Actor receive loop
	def receive = {

		// message was sent; add the timestamps to the map
		case MessageSent(corrId, startSend, endSend, checks, session, next, title) => {
			receivedMessages.get(corrId) match {
				case Some((received, message)) => {
					// message was received out of order, lets just deal with it
					processMessage(session, startSend, received, endSend, checks, message, next, title)
					receivedMessages -= corrId
				}
				case None => {
					// normal path
					sentMessages += corrId -> (startSend, endSend, checks, session, next, title)
				}
			}
		}

		// message was received; publish to the datawriter and remove from the hashmap
		case MessageReceived(corrId, received, message) => {
			sentMessages.get(corrId) match {
				case Some((startSend, endSend, checks, session, next, title)) => {
					processMessage(session, startSend, received, endSend, checks, message, next, title)
					sentMessages -= corrId
				}
				case None => {
					// failed to find message; early receive? or bad return correlation id?
					// let's add it to the received messages buffer just in case
					receivedMessages += corrId -> (received, message)
				}
			}
		}
	}

	/**
	 * Processes a matched message
	 */
	def processMessage(session: Session, startSend: Long, received: Long, endSend: Long,
		checks: List[JmsCheck], message: Message, next: ActorRef, title: String) = {

		// run all of the checks
		val checksPassed = checks.forall((check: JmsCheck) => check(message))
		val gatling_response = if (checksPassed) OK else KO

		// advise the Gatling API that it is complete and move to next
		DataWriter.tell(RequestMessage(session.scenarioName, session.userId, Nil, title,
			startSend, received, endSend, received, gatling_response, None, Nil))
		next ! session
	}
}
