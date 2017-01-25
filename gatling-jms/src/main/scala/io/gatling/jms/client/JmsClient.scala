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
package io.gatling.jms.client

import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request.{ JmsDestination, JmsQueue, JmsTemporaryQueue, JmsTemporaryTopic, JmsTopic }

import com.typesafe.scalalogging.StrictLogging
import javax.jms._

import scala.util.control.NonFatal

import io.gatling.commons.model.Credentials

object JmsClient {

  def apply(protocol: JmsProtocol, destination: JmsDestination) = {
    new JmsSendClient(protocol, destination)
  }

  def apply(protocol: JmsProtocol, destination: JmsDestination, replyDestination: JmsDestination) = {
    new JmsRequestReplyClient(protocol, destination, replyDestination)
  }
}

abstract class JmsClient(
    connectionFactory: ConnectionFactory,
    destination:       JmsDestination,
    credentials:       Option[Credentials],
    deliveryMode:      Int
) extends StrictLogging {

  val conn = credentials match {
    case Some(creds) => connectionFactory.createConnection(creds.username, creds.password)
    case _           => connectionFactory.createConnection
  }

  conn.start()

  val session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)
  logger.info(s"Got Connection $conn")

  //  target destination/producer
  val producer = session.createProducer(createDestination(destination))

  // delivery mode based on input from caller
  producer.setDeliveryMode(deliveryMode)

  def createDestination(destination: JmsDestination): Destination =
    destination match {
      case JmsQueue(name)    => session.createQueue(name)
      case JmsTopic(name)    => session.createTopic(name)
      case JmsTemporaryQueue => session.createTemporaryQueue()
      case JmsTemporaryTopic => session.createTemporaryTopic()
    }

  /**
   * Writes a property map to the message properties
   */
  private def writePropsToMessage(props: Map[String, Any], message: Message): Unit =
    props.foreach { case (key, value) => message.setObjectProperty(key, value) }

  /**
   * Wrapper to send a BytesMessage, returns the message ID of the sent message
   */
  def sendBytesMessage(bytes: Array[Byte], props: Map[String, Any], jmsType: Option[String], beforeSend: Message => Unit): Unit = {
    val message = session.createBytesMessage
    message.writeBytes(bytes)
    writePropsToMessage(props, message)
    jmsType.foreach(message.setJMSType)
    sendMessage(message, beforeSend)
  }

  /**
   * Wrapper to send a MapMessage, returns the message ID of the sent message
   * <p>
   * Note that map must match the javax.jms.MapMessage contract ie: "This method works only
   * for the objectified primitive object types (Integer, Double, Long ...), String objects,
   * and byte arrays."
   */
  def sendMapMessage(map: Map[String, Any], props: Map[String, Any], jmsType: Option[String], beforeSend: Message => Unit): Unit = {
    val message = session.createMapMessage
    map.foreach { case (key, value) => message.setObject(key, value) }
    writePropsToMessage(props, message)
    jmsType.foreach(message.setJMSType)
    sendMessage(message, beforeSend)
  }

  /**
   * Wrapper to send an ObjectMessage, returns the message ID of the sent message
   */
  def sendObjectMessage(o: java.io.Serializable, props: Map[String, Any], jmsType: Option[String], beforeSend: Message => Unit): Unit = {
    val message = session.createObjectMessage(o)
    writePropsToMessage(props, message)
    jmsType.foreach(message.setJMSType)
    sendMessage(message, beforeSend)
  }

  /**
   * Wrapper to send a TextMessage, returns the message ID of the sent message
   */
  def sendTextMessage(messageText: String, props: Map[String, Any], jmsType: Option[String], beforeSend: Message => Unit): Unit = {
    val message = session.createTextMessage(messageText)
    writePropsToMessage(props, message)
    jmsType.foreach(message.setJMSType)
    sendMessage(message, beforeSend)
  }

  private def sendMessage(message: Message, beforeSend: Message => Unit): Unit = {
    prepareMessage(message)
    beforeSend(message)
    producer.send(message)
  }

  protected def prepareMessage(message: Message): Unit

  def close(): Unit = {
    try {
      producer.close()
      session.close()
      conn.stop()
    } catch {
      case NonFatal(e) => logger.debug("Exception while closing JmsReplyClient: " + e.getMessage)
    }
  }
}
