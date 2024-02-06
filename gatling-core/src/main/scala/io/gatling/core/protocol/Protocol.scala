/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

object Protocol {
  def indexByType(protocols: Iterable[Protocol]): Protocols =
    protocols.map(p => p.getClass.asInstanceOf[Class[Protocol]] -> p).toMap
}

trait Protocol

trait ProtocolKey[P <: Protocol, C <: ProtocolComponents] {
  def protocolClass: Class[Protocol]

  def defaultProtocolValue(configuration: GatlingConfiguration): P
  def newComponents(coreComponents: CoreComponents): P => C
}

object ProtocolComponents {
  val NoopOnExit: Session => Unit = _ => ()
}

trait ProtocolComponents {
  def onStart: Session => Session
  def onExit: Session => Unit
}

final class ProtocolComponentsRegistries(coreComponents: CoreComponents, globalProtocols: Protocols) {
  private val componentsFactoryCache = mutable.Map.empty[ProtocolKey[_, _], Protocol => ProtocolComponents]
  private val defaultProtocolValueCache = mutable.Map.empty[ProtocolKey[_, _], Protocol]

  def scenarioRegistry(scenarioProtocols: Protocols): ProtocolComponentsRegistry =
    new ProtocolComponentsRegistry(
      coreComponents,
      globalProtocols ++ scenarioProtocols,
      componentsFactoryCache,
      defaultProtocolValueCache
    )
}

final class ProtocolComponentsRegistry(
    coreComponents: CoreComponents,
    protocols: Protocols,
    componentsFactoryCache: mutable.Map[ProtocolKey[_, _], Protocol => ProtocolComponents],
    defaultProtocolValueCache: mutable.Map[ProtocolKey[_, _], Protocol]
) {
  private val componentsCache = mutable.Map.empty[ProtocolKey[_, _], ProtocolComponents]

  private def computeComponentsFactory(key: ProtocolKey[_, _]): Protocol => ProtocolComponents =
    key.newComponents(coreComponents).asInstanceOf[Protocol => ProtocolComponents]

  private def computeDefaultProtocolValue[P <: Protocol](key: ProtocolKey[P, _]): P =
    defaultProtocolValueCache.getOrElseUpdate(key, key.defaultProtocolValue(coreComponents.configuration)).asInstanceOf[P]

  private def computeComponent[P <: Protocol, C <: ProtocolComponents](key: ProtocolKey[P, C]): ProtocolComponents = {
    val componentsFactory = componentsFactoryCache.getOrElseUpdate(key, computeComponentsFactory(key))
    val protocol = protocols.getOrElse(key.protocolClass, computeDefaultProtocolValue(key))
    componentsFactory(protocol)
  }

  def components[P <: Protocol, C <: ProtocolComponents](key: ProtocolKey[P, C]): C =
    componentsCache.getOrElseUpdate(key, computeComponent(key)).asInstanceOf[C]

  def onStart: Session => Session =
    componentsCache.values.foldLeft(Session.Identity)(_ andThen _.onStart)

  def onExit: Session => Unit =
    session => componentsCache.values.foreach(_.onExit(session))
}
