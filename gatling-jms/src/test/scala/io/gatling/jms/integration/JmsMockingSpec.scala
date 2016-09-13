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
package io.gatling.jms.integration

import javax.jms.{ Message, MessageListener }

import scala.concurrent.duration._

import io.gatling.core.CoreComponents
import io.gatling.core.action.{ Action, ActorDelegatingAction }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.pause.Constant
import io.gatling.core.protocol.{ ProtocolComponentsRegistries, Protocols }
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.{ ScenarioBuilder, ScenarioContext }
import io.gatling.jms._
import io.gatling.jms.client.{ BrokerBasedSpec, JmsReqReplyClient }
import io.gatling.jms.request.JmsDestination

import akka.actor.ActorRef
import org.apache.activemq.jndi.ActiveMQInitialContextFactory

class JmsMockCustomer(client: JmsReqReplyClient, mockResponse: PartialFunction[Message, String]) extends MessageListener {

  val producer = client.session.createProducer(null)
  client.createReplyConsumer().setMessageListener(this)

  override def onMessage(request: Message): Unit = {
    if (mockResponse.isDefinedAt(request)) {
      val response = client.session.createTextMessage(mockResponse(request))
      response.setJMSCorrelationID(request.getJMSMessageID)
      producer.send(request.getJMSReplyTo, response)
    }
  }

  def close(): Unit = {
    producer.close()
    client.close()
  }
}

trait JmsMockingSpec extends BrokerBasedSpec with JmsDsl {

  def jmsProtocol = jms
    .connectionFactoryName("ConnectionFactory")
    .url("vm://gatling?broker.persistent=false&broker.useJmx=false")
    .contextFactory(classOf[ActiveMQInitialContextFactory].getName)
    .listenerCount(1)

  def runScenario(sb: ScenarioBuilder, timeout: FiniteDuration = 10.seconds, protocols: Protocols = Protocols(jmsProtocol))(implicit configuration: GatlingConfiguration) = {
    val coreComponents = CoreComponents(mock[ActorRef], mock[Throttler], mock[StatsEngine], mock[Action], configuration)
    val next = new ActorDelegatingAction("next", self)
    val protocolComponentsRegistry = new ProtocolComponentsRegistries(system, coreComponents, protocols).scenarioRegistry(Protocols(Nil))
    val actor = sb.build(ScenarioContext(system, coreComponents, protocolComponentsRegistry, Constant, throttled = false), next)
    actor ! Session("TestSession", 0)
    val session = expectMsgClass(timeout, classOf[Session])

    session
  }

  def jmsMock(queue: JmsDestination, f: PartialFunction[Message, String]): Unit = {
    val processor = new JmsMockCustomer(createClient(queue), f)
    registerCleanUpAction(() => processor.close())
  }
}
