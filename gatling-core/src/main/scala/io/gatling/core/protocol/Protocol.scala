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
package io.gatling.core.protocol

import scala.collection.mutable

import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session

import akka.actor.ActorSystem

/**
 * This trait is a model to all protocol specific configuration
 */
trait Protocol

trait ProtocolKey {
  type Protocol
  type Components
  def protocolClass: Class[io.gatling.core.protocol.Protocol]

  def defaultProtocolValue(configuration: GatlingConfiguration): Protocol
  def newComponents(system: ActorSystem, coreComponents: CoreComponents): Protocol => Components
}

trait ProtocolComponents {
  def onStart: Option[Session => Session]
  def onExit: Option[Session => Unit]
}

class ProtocolComponentsRegistries(system: ActorSystem, coreComponents: CoreComponents, globalProtocols: Protocols) {

  val componentsFactoryCache = mutable.Map.empty[ProtocolKey, Any]

  def scenarioRegistry(scenarioProtocols: Protocols): ProtocolComponentsRegistry =
    new ProtocolComponentsRegistry(
      system,
      coreComponents,
      globalProtocols ++ scenarioProtocols,
      componentsFactoryCache
    )
}

class ProtocolComponentsRegistry(system: ActorSystem, coreComponents: CoreComponents, protocols: Protocols, componentsFactoryCache: mutable.Map[ProtocolKey, Any]) {

  val protocolCache = mutable.Map.empty[ProtocolKey, Protocol]
  val componentsCache = mutable.Map.empty[ProtocolKey, Any]

  def components(key: ProtocolKey): key.Components = {

      def componentsFactory = componentsFactoryCache.getOrElseUpdate(key, key.newComponents(system, coreComponents)).asInstanceOf[key.Protocol => key.Components]
      def protocol: key.Protocol = protocolCache.getOrElse(key, protocols.protocols.getOrElse(key.protocolClass, key.defaultProtocolValue(coreComponents.configuration))).asInstanceOf[key.Protocol]
      def comps = componentsFactory(protocol)

    componentsCache.getOrElseUpdate(key, comps).asInstanceOf[key.Components]
  }

  def onStart: Session => Session =
    componentsCache.values.collect { case protocolComponents: ProtocolComponents => protocolComponents.onStart }.flatten.toList match {
      case Nil          => Session.Identity
      case head :: tail => tail.foldLeft(head)(_ andThen _)
    }

  def onExit: Session => Unit =
    componentsCache.values.collect { case any: ProtocolComponents => any.onExit }.flatten.toList match {
      case Nil => _ => ()
      case onExits => session => onExits.foreach(_(session))
    }
}
