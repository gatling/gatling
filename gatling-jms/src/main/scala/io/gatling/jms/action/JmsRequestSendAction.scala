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

import javax.jms.Message

import io.gatling.commons.stats.{ OK, Status }
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.validation._
import io.gatling.core.action._
import io.gatling.core.session.{ Expression, Session, resolveOptionalExpression }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.util.NameGen
import io.gatling.jms.client.JmsClient
import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request._

/**
 * Core JMS Action to handle Send
 *
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send.
 */
class JmsRequestSendAction(attributes: JmsAttributes, protocol: JmsProtocol, val statsEngine: StatsEngine, val next: Action)
    extends ExitableAction with NameGen {

  override val name = genName("jmsSend")

  // Create a client to refer to
  val client = JmsClient(protocol, attributes.destination)

  /**
   * Framework calls the execute() method to send a single request
   * <p>
   * Note this does not catch any exceptions (even JMSException) as generally these indicate a
   * configuration failure that is unlikely to be addressed by retrying with another message
   */
  override def execute(session: Session): Unit = recover(session) {

    val messageProperties = resolveProperties(attributes.messageProperties, session)

    val jmsType = resolveOptionalExpression(attributes.jmsType, session)

    // sendtime
    val startDate = nowMillis

    val msg = jmsType.flatMap(jmsType =>
      messageProperties.flatMap { props =>
        attributes.message match {
          case BytesJmsMessage(bytes) => bytes(session).map(bytes => client.sendBytesMessage(bytes, props, jmsType))
          case MapJmsMessage(map)     => map(session).map(map => client.sendMapMessage(map, props, jmsType))
          case ObjectJmsMessage(o)    => o(session).map(o => client.sendObjectMessage(o, props, jmsType))
          case TextJmsMessage(txt)    => txt(session).map(txt => client.sendTextMessage(txt, props, jmsType))
        }
      })

    // done time
    val endDate = nowMillis

    msg.map { msg =>
      if (logger.underlying.isDebugEnabled()) {
        logMessage(s"Message sent JMSMessageID=${msg.getJMSMessageID}", msg)
      }
      executeNext(session, startDate, endDate, OK, next, attributes.requestName)
    }
  }

  private def executeNext(
    session:  Session,
    sent:     Long,
    received: Long,
    status:   Status,
    next:     Action,
    title:    String,
    message:  Option[String] = None
  ) = {
    val timings = ResponseTimings(sent, received)
    statsEngine.logResponse(session, title, timings, status, None, message)
    next ! session.logGroupRequest(timings.responseTime, status).increaseDrift(nowMillis - received)
  }

  def resolveProperties(
    properties: Map[Expression[String], Expression[Any]],
    session:    Session
  ): Validation[Map[String, Any]] = {
    properties.foldLeft(Map.empty[String, Any].success) {
      case (resolvedProperties, (key, value)) =>
        for {
          key <- key(session)
          value <- value(session)
          resolvedProperties <- resolvedProperties
        } yield resolvedProperties + (key -> value)
    }
  }

  def logMessage(text: String, msg: Message): Unit = {
    logger.debug(text)
    logger.trace(msg.toString)
  }
}
