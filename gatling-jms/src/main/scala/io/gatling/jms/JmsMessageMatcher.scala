package io.gatling.jms

import javax.jms.Message

object JmsDefaultMessageMatcher extends JmsMessageMatcher {

  override def request(msg: Message): String = msg.getJMSMessageID

  override def response(msg: Message): String = msg.getJMSCorrelationID
}

/**
 * define trait for message matching logic with separate request/response
 * to see how it can be used check JmsDefaultMessageMatcher
 */
trait JmsMessageMatcher {
  def request(msg: Message): String
  def response(msg: Message): String
}
