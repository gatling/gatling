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
package io.gatling.jms.protocol

import io.gatling.core.CoreComponents
import io.gatling.core.config.{ GatlingConfiguration, Credentials }
import io.gatling.core.protocol.{ ProtocolKey, Protocol }
import io.gatling.jms.action.JmsRequestTrackerActor

import akka.actor.ActorSystem

object JmsProtocol {

  val JmsProtocolKey = new ProtocolKey {

    type Protocol = JmsProtocol
    type Components = JmsComponents

    def protocolClass: Class[io.gatling.core.protocol.Protocol] = classOf[JmsProtocol].asInstanceOf[Class[io.gatling.core.protocol.Protocol]]

    def defaultProtocolValue(configuration: GatlingConfiguration): JmsProtocol = throw new IllegalStateException("Can't provide a default value for JmsProtocol")

    def newComponents(system: ActorSystem, coreComponents: CoreComponents): JmsProtocol => JmsComponents = {
      val tracker = system.actorOf(JmsRequestTrackerActor.props(coreComponents.statsEngine, coreComponents.configuration), "jmsRequestTracker")
      jmsProtocol => JmsComponents(jmsProtocol, tracker)
    }
  }
}

case class JmsProtocol(
    contextFactory:        String,
    connectionFactoryName: String,
    url:                   String,
    credentials:           Option[Credentials],
    anonymousConnect:      Boolean,
    listenerCount:         Int,
    deliveryMode:          Int,
    receiveTimeout:        Option[Long],
    messageMatcher:        JmsMessageMatcher
) extends Protocol {

  type Components = JmsComponents
}
