/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.jakarta.jms.action

import io.gatling.commons.stats.OK
import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.action._
import io.gatling.core.actor.ActorRef
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.jakarta.jms.client.JmsConnectionPool
import io.gatling.jakarta.jms.protocol.JmsProtocol
import io.gatling.jakarta.jms.request._

import jakarta.jms.Message

/**
 * Core JMS Action to handle Send
 *
 * This handles the core "send"ing of messages. Gatling calls the execute method to trigger a send.
 */
final class Send(
    attributes: JmsAttributes,
    protocol: JmsProtocol,
    jmsConnectionPool: JmsConnectionPool,
    val statsEngine: StatsEngine,
    val clock: Clock,
    val next: Action,
    throttler: Option[ActorRef[Throttler.Command]]
) extends JmsAction(attributes, protocol, jmsConnectionPool, throttler) {
  override val name: String = genName("jmsSend")

  override protected def aroundSend(requestName: String, session: Session, message: Message): Validation[Around] =
    new Around(
      before = () => {
        if (logger.underlying.isDebugEnabled) {
          logMessage(s"Message sent JMSMessageID=${message.getJMSMessageID}", message)
        }

        val now = clock.nowMillis
        statsEngine.logResponse(session.scenario, session.groups, requestName, now, now, OK, None, None)
        next ! session
      },
      after = () => ()
    ).success
}
