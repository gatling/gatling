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

import io.gatling.commons.stats.{ OK, Status }
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.core.action._
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.util.NameGen
import io.gatling.jms.client.{ JmsClient, JmsSendClient }
import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request._

/**
 * Core JMS Action to handle Send
 *
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send.
 */
class JmsRequestSend(val attributes: JmsAttributes, protocol: JmsProtocol, val statsEngine: StatsEngine, val next: Action)
    extends ExitableAction with JmsAction[JmsSendClient] with NameGen {

  override val name = genName("jmsSend")

  // Create a client to refer to
  override val client = JmsClient(protocol, attributes.destination)

  /**
   * Framework calls the execute() method to send a single request
   * <p>
   * Note this does not catch any exceptions (even JMSException) as generally these indicate a
   * configuration failure that is unlikely to be addressed by retrying with another message
   */
  override def execute(session: Session): Unit = recover(session) {
    sendMessage(session) {
      case (msg, startDate) =>
        // done time
        val endDate = nowMillis
        if (logger.underlying.isDebugEnabled) {
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
}
