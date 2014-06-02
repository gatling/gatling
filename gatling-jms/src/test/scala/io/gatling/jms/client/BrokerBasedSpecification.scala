/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jms.client

import org.specs2.mutable.Specification
import org.specs2.specification.{ Step, Fragments }
import org.apache.activemq.broker.{ BrokerFactory, BrokerService }
import io.gatling.jms.{ MessageIDMessageMatcher, JmsDestination }
import org.apache.activemq.jndi.ActiveMQInitialContextFactory

trait BrokerBasedSpecification extends Specification {

  /** the map method allows to "post-process" the fragments after their creation */
  override def map(fs: => Fragments) = Step(startBroker()) ^ fs ^ Step(stopBroker())

  lazy val broker: BrokerService = BrokerFactory.createBroker("broker://()/gatling?persistent=false&useJmx=false")

  def startBroker() = {
    {
      broker.start()
      broker.waitUntilStarted()
    }
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
      classOf[ActiveMQInitialContextFactory].getName,
      1,
      MessageIDMessageMatcher)
  }
}
