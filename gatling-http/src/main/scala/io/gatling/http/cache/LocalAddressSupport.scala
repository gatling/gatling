/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.CircularIterator
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.protocol.HttpProtocol

private[http] object LocalAddressSupport {

  private val LocalAddressAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.localAddress"

  def setLocalAddress(httpProtocol: HttpProtocol): Session => Session = {
    httpProtocol.enginePart.localAddresses match {
      case Nil            => Session.Identity
      case address :: Nil => _.set(LocalAddressAttributeName, address)
      case addresses =>
        val it = CircularIterator(addresses.toVector, threadSafe = true)
        _.set(LocalAddressAttributeName, it.next())
    }
  }

  val localAddress: Session => Option[InetAddress] =
    _.attributes.get(LocalAddressAttributeName).map(_.asInstanceOf[InetAddress])
}
