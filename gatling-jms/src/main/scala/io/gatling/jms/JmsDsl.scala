/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.jms

import javax.jms.ConnectionFactory

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.jms.check.JmsCheckSupport
import io.gatling.jms.jndi.{ JmsJndiConnectionFactoryBuilder, JmsJndiConnectionFactoryBuilderBase }
import io.gatling.jms.protocol.{ JmsProtocol, JmsProtocolBuilder, JmsProtocolBuilderBase }
import io.gatling.jms.request._

trait JmsDsl extends JmsCheckSupport {

  def jms(implicit configuration: GatlingConfiguration): JmsProtocolBuilderBase.type = JmsProtocolBuilderBase

  val jmsJndiConnectionFactory: JmsJndiConnectionFactoryBuilderBase.type = JmsJndiConnectionFactoryBuilderBase

  /**
   * DSL text to start the jms builder
   *
   * @param requestName human readable name of request
   * @return a JmsDslBuilderBase instance which can be used to build up a JMS action
   */
  def jms(requestName: Expression[String]): JmsDslBuilderBase = new JmsDslBuilderBase(requestName)

  /**
   * Convert a JmsProtocolBuilder to a JmsProtocol
   * <p>
   * Simplifies the API somewhat (you can pass the builder reference to the scenario .protocolConfig() method)
   */
  implicit def jmsProtocolBuilder2jmsProtocol(builder: JmsProtocolBuilder): JmsProtocol = builder.build

  implicit def jmsDslBuilder2ActionBuilder(builder: SendDslBuilder): ActionBuilder = builder.build

  implicit def jmsDslBuilder2ActionBuilder(builder: RequestReplyDslBuilder): ActionBuilder = builder.build

  implicit def jmsJndiConnectionFactory2ActionBuilder(builder: JmsJndiConnectionFactoryBuilder): ConnectionFactory = builder.build()

  def topic(name: Expression[String]): JmsDestination = JmsTopic(name)
  def queue(name: Expression[String]): JmsDestination = JmsQueue(name)
}
