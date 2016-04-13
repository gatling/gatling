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
package io.gatling.http.cache

import java.net.InetAddress

import io.gatling.commons.util.RoundRobin
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.util.HttpTypeHelper

object LocalAddressSupport {

  val LocalAddressAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.localAddress"
}

trait LocalAddressSupport {

  import LocalAddressSupport._

  def configuration: GatlingConfiguration

  def setLocalAddress(httpProtocol: HttpProtocol): Session => Session = {
    httpProtocol.enginePart.localAddresses match {
      case Nil                 => identity
      case localAddress :: Nil => _.set(LocalAddressAttributeName, localAddress)
      case localAddresses =>
        val it = RoundRobin(localAddresses.toVector)
        _.set(LocalAddressAttributeName, it.next())
    }
  }

  val localAddress: (Session => Option[InetAddress]) = {
    // import optimized TypeCaster
    import HttpTypeHelper._
    _(LocalAddressAttributeName).asOption[InetAddress]
  }
}
