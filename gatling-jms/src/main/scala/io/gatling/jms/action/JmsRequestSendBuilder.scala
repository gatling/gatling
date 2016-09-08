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

import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.structure.ScenarioContext
import io.gatling.jms.protocol.{ JmsComponents, JmsProtocol }
import io.gatling.jms.request.JmsAttributes

case class JmsRequestSendBuilder(attributes: JmsAttributes, configuration: GatlingConfiguration) extends ActionBuilder {

  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry): JmsComponents =
    protocolComponentsRegistry.components(JmsProtocol.JmsProtocolKey)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val jmsComponents = components(protocolComponentsRegistry)
    val statsEngine = coreComponents.statsEngine
    new JmsRequestSend(attributes, jmsComponents.jmsProtocol, statsEngine, next)
  }
}
