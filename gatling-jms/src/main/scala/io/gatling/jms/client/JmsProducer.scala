/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import javax.jms.{ Message, MessageProducer, Session => JmsSession }

class JmsProducer(jmsSession: JmsSession, producer: MessageProducer) {

  /**
   * Wrapper to send a BytesMessage, returns the message ID of the sent message
   */
  def sendBytesMessage(bytes: Array[Byte], props: Map[String, Any], jmsType: Option[String], beforeSend: Message => Unit): Unit = {
    val message = jmsSession.createBytesMessage
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
    val message = jmsSession.createMapMessage
    map.foreach { case (key, value) => message.setObject(key, value) }
    writePropsToMessage(props, message)
    jmsType.foreach(message.setJMSType)
    sendMessage(message, beforeSend)
  }

  /**
   * Wrapper to send an ObjectMessage, returns the message ID of the sent message
   */
  def sendObjectMessage(o: java.io.Serializable, props: Map[String, Any], jmsType: Option[String], beforeSend: Message => Unit): Unit = {
    val message = jmsSession.createObjectMessage(o)
    writePropsToMessage(props, message)
    jmsType.foreach(message.setJMSType)
    sendMessage(message, beforeSend)
  }

  /**
   * Wrapper to send a TextMessage, returns the message ID of the sent message
   */
  def sendTextMessage(messageText: String, props: Map[String, Any], jmsType: Option[String], beforeSend: Message => Unit): Unit = {
    val message = jmsSession.createTextMessage(messageText)
    writePropsToMessage(props, message)
    jmsType.foreach(message.setJMSType)
    sendMessage(message, beforeSend)
  }

  private def sendMessage(message: Message, beforeSend: Message => Unit): Unit = {
    beforeSend(message)
    producer.send(message)
  }

  /**
   * Writes a property map to the message properties
   */
  private def writePropsToMessage(props: Map[String, Any], message: Message): Unit =
    props.foreach { case (key, value) => message.setObjectProperty(key, value) }
}
