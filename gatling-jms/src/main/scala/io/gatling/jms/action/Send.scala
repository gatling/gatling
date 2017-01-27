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

import javax.jms.Message

import io.gatling.commons.stats.OK
import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.core.action._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.util.NameGen
import io.gatling.jms.client.{ JmsClient, JmsSendClient }
import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request._

import akka.actor.ActorSystem

/**
 * Core JMS Action to handle Send
 *
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send.
 */
class Send(val attributes: JmsAttributes, protocol: JmsProtocol, system: ActorSystem, val statsEngine: StatsEngine, configuration: GatlingConfiguration, val next: Action)
    extends JmsAction[JmsSendClient] with NameGen {

  override val name = genName("jmsSend")

  override val client = JmsClient(protocol, attributes.destination)

  system.registerOnTermination {
    client.close()
  }

  protected override def beforeSend(requestName: String, session: Session)(message: Message): Unit = {
    val now = nowMillis
    if (logger.underlying.isDebugEnabled) {
      logMessage(s"Message sent JMSMessageID=${message.getJMSMessageID}", message)
    }

    val timings = ResponseTimings(now, now)
    configuration.resolve(
      // [fl]
      //
      //
      //
      //
      // [fl]
      statsEngine.logResponse(session, requestName, timings, OK, None, None)
    )
    next ! session.logGroupRequest(timings.responseTime, OK)
  }
}
