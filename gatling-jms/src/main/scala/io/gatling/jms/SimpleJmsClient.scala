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

import io.gatling.core.Predef._
import io.gatling.core.action.Chainable
import akka.actor.{ ActorRef, Actor, Props }
import io.gatling.core.result.writer.DataWriter
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.result.writer.RequestMessage
import io.gatling.core.result.message.OK
import java.util.{ Hashtable => JHashtable }
import javax.naming._
import javax.jms._

/**
 * Trivial JMS client, allows sending messages and use of a MessageListener
 * @author jasonk@bluedevel.com
 */
class SimpleJmsClient(val qcfName: String, val queueName: String, val url: String,
	val username: Option[String], val password: Option[String], val contextFactory: String,
	val deliveryMode: Int) {

	// create InitialContext
	val properties = new JHashtable[String, String]
	properties.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory)
	properties.put(Context.PROVIDER_URL, url)
	username match {
		case None => None
		case Some(s) => properties.put(Context.SECURITY_PRINCIPAL, s)
	}
	password match {
		case None => None
		case Some(s) => properties.put(Context.SECURITY_CREDENTIALS, s)
	}

	val ctx = new InitialContext(properties)
	println("Got InitialContext " + ctx.toString())

	// create QueueConnectionFactory
	val qcf = (ctx.lookup(qcfName)).asInstanceOf[ConnectionFactory]
	println("Got ConnectionFactory " + qcf.toString())

	// create QueueConnection
	val conn = qcf.createConnection()
	conn.start()
	val session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)
	println("Got Connection " + conn.toString())

	// reply queue and target destination/producer
	val replyQ = session.createTemporaryQueue()
	val destination = session.createQueue(queueName)
	val producer = session.createProducer(destination)

	// delivery mode based on input from caller
	producer.setDeliveryMode(deliveryMode)

	/**
	 * Gets a new consumer for the reply queue
	 */
	def createReplyConsumer = conn.createSession(false, Session.AUTO_ACKNOWLEDGE).createConsumer(replyQ)

	/**
	 * Writes a property map to the message properties
	 */
	private def writePropsToMessage(props: Map[String, Object], message: Message) = {
		props.foreach {
			case (key: String, value: Object) => message.setObjectProperty(key, value)
		}
	}

	/**
	 * Wrapper to send a BytesMessage, returns the message ID of the sent message
	 */
	def sendBytesMessage(bytes: Array[Byte], props: Map[String, Object]): String = {
		val message = session.createBytesMessage
		message.writeBytes(bytes)
		writePropsToMessage(props, message)
		sendMessage(message)
	}

	/**
	 * Wrapper to send a MapMessage, returns the message ID of the sent message
	 * <p>
	 * Note that map must match the javax.jms.MapMessage contract ie: "This method works only
	 * for the objectified primitive object types (Integer, Double, Long ...), String objects,
	 * and byte arrays."
	 */
	def sendMapMessage(map: Map[String, Object], props: Map[String, Object]): String = {
		val message = session.createMapMessage
		map.foreach {
			case (key: String, value: Object) => message.setObject(key, value)
		}
		writePropsToMessage(props, message)
		sendMessage(message)
	}

	/**
	 * Wrapper to send an ObjectMessage, returns the message ID of the sent message
	 */
	def sendObjectMessage(o: java.io.Serializable, props: Map[String, Object]): String = {
		val message = session.createObjectMessage(o)
		writePropsToMessage(props, message)
		sendMessage(message)
	}

	/**
	 * Wrapper to send a TextMessage, returns the message ID of the sent message
	 */
	def sendTextMessage(messageText: String, props: Map[String, Object]): String = {
		val message = session.createTextMessage(messageText)
		writePropsToMessage(props, message)
		sendMessage(message)
	}

	/**
	 * Sends a JMS message, returns the message ID of the sent message
	 * <p>
	 * Note that exceptions are allowed to bubble up to the caller
	 */
	def sendMessage(message: Message): String = {

		message.setJMSReplyTo(replyQ)
		producer.send(message)

		// return the message id
		message.getJMSMessageID

	}

}

