package io.gatling.jms.client

import javax.jms.{ Message, MessageConsumer }

import io.gatling.jms.{ JmsDestination, JmsProtocol }

object JmsClient {
  def apply(protocol: JmsProtocol, destination: JmsDestination, replyDestination: JmsDestination): JmsClient = {
    new SimpleJmsClient(protocol.connectionFactoryName,
      destination,
      replyDestination,
      protocol.url,
      protocol.credentials,
      protocol.anonymousConnect,
      protocol.contextFactory,
      protocol.deliveryMode,
      protocol.messageMatcher)
  }
}

trait JmsClient {

  /**
   * Gets a new consumer for the reply queue
   */
  def createReplyConsumer(selector: String = null): MessageConsumer

  /**
   * Wrapper to send a BytesMessage, returns the message ID of the sent message
   */
  def sendBytesMessage(bytes: Array[Byte], props: Map[String, Any]): Message

  /**
   * Wrapper to send a MapMessage, returns the message ID of the sent message
   * <p>
   * Note that map must match the javax.jms.MapMessage contract ie: "This method works only
   * for the objectified primitive object types (Integer, Double, Long ...), String objects,
   * and byte arrays."
   */
  def sendMapMessage(map: Map[String, Any], props: Map[String, Any]): Message

  /**
   * Wrapper to send an ObjectMessage, returns the message ID of the sent message
   */
  def sendObjectMessage(o: java.io.Serializable, props: Map[String, Any]): Message

  /**
   * Wrapper to send a TextMessage, returns the message ID of the sent message
   */
  def sendTextMessage(messageText: String, props: Map[String, Any]): Message

  def close(): Unit

}
