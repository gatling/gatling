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
package io.gatling.jms.client

import io.gatling.jms.protocol.MessageIDMessageMatcher
import io.gatling.jms.request.JmsDestination

import org.apache.activemq.broker.{ BrokerFactory, BrokerService }
import org.apache.activemq.jndi.ActiveMQInitialContextFactory

import io.gatling.AkkaSpec

trait BrokerBasedSpec extends AkkaSpec {

  override def beforeAll() = startBroker()

  override def afterAll() = {
    super.afterAll()
    cleanUpActions.foreach(f => f())
    stopBroker()
  }

  var cleanUpActions: List[(() => Unit)] = Nil
  lazy val broker: BrokerService = BrokerFactory.createBroker("broker://()/gatling?persistent=false&useJmx=false")

  def startBroker() = {
    broker.start()
    broker.waitUntilStarted()
  }

  def stopBroker() = {
    broker.stop()
    broker.waitUntilStopped()
  }

  def createClient(destination: JmsDestination) = {
    new SimpleJmsClient(
      "ConnectionFactory",
      destination,
      destination,
      "vm://gatling?broker.persistent=false&broker.useJmx=false",
      None,
      false,
      classOf[ActiveMQInitialContextFactory].getName,
      1,
      MessageIDMessageMatcher
    )
  }
}
