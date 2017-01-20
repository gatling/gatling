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

import javax.jms._

import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request.JmsDestination

/**
 * Trivial JMS client, allows sending messages and use of a MessageListener
 * @author jasonk@bluedevel.com
 */
class JmsRequestReplyClient(
  protocol:         JmsProtocol,
  destination:      JmsDestination,
  replyDestination: JmsDestination
)
    extends JmsClient(
      protocol.connectionFactory,
      destination,
      protocol.credentials,
      protocol.deliveryMode
    ) {

  // reply queue and target destination/producer
  val replyJmsDestination = createDestination(replyDestination)

  val replyDestinationName = replyJmsDestination.toString

  /**
   * Gets a new consumer for the reply queue
   */
  def createReplyConsumer(selector: String = null): MessageConsumer =
    conn.createSession(false, Session.AUTO_ACKNOWLEDGE).createConsumer(replyJmsDestination, selector)

  override protected def prepareMessage(message: Message): Unit = {
    message.setJMSReplyTo(replyJmsDestination)
    protocol.messageMatcher.prepareRequest(message)
  }
}
