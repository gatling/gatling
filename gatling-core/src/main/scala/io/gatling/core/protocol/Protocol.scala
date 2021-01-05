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

trait ProtocolKey[P, C] {
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

class ProtocolComponentsRegistries(coreComponents: CoreComponents, globalProtocols: Protocols) {

  private val componentsFactoryCache = mutable.Map.empty[ProtocolKey[_, _], Any]

  def scenarioRegistry(scenarioProtocols: Protocols): ProtocolComponentsRegistry =
    new ProtocolComponentsRegistry(
      coreComponents,
      globalProtocols ++ scenarioProtocols,
      componentsFactoryCache
    )
}

class ProtocolComponentsRegistry(coreComponents: CoreComponents, protocols: Protocols, componentsFactoryCache: mutable.Map[ProtocolKey[_, _], Any]) {

  private val protocolCache = mutable.Map.empty[ProtocolKey[_, _], Protocol]
  private val componentsCache = mutable.Map.empty[ProtocolKey[_, _], ProtocolComponents]

  def components[P, C](key: ProtocolKey[P, C]): C = {

    def componentsFactory: P => C = componentsFactoryCache.getOrElseUpdate(key, key.newComponents(coreComponents)).asInstanceOf[P => C]
    def protocol: P =
      protocolCache.getOrElse(key, protocols.getOrElse(key.protocolClass, key.defaultProtocolValue(coreComponents.configuration))).asInstanceOf[P]
    def comps: C = componentsFactory(protocol)

    componentsCache.getOrElseUpdate(key, comps.asInstanceOf[ProtocolComponents]).asInstanceOf[C]
  }

  def onStart: Session => Session =
    if (componentsCache.values.nonEmpty) {
      componentsCache.values.map(_.onStart).reduceLeft(_ andThen _)
    } else {
      Session.Identity
    }

  def onExit: Session => Unit =
    session => componentsCache.values.foreach(_.onExit(session))
}
