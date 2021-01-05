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

package io.gatling.http.cache

import java.net.InetAddress

import io.gatling.commons.util.CircularIterator
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.protocol.HttpProtocol

private[http] object LocalAddressSupport {

  private val LocalIpV4AddressAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.localIpV4Address"
  private val LocalIpV6AddressAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.localIpV6Address"

  def setLocalAddresses(httpProtocol: HttpProtocol): Session => Session = {
    val ipV4Addresses = httpProtocol.enginePart.localIpV4Addresses
    val ipV6Addresses = httpProtocol.enginePart.localIpV6Addresses

    ipV4Addresses match {
      case Nil =>
        ipV6Addresses match {
          case Nil =>
            Session.Identity
          case singleIpV6Address :: Nil =>
            _.set(LocalIpV6AddressAttributeName, singleIpV6Address)
          case ipV6Addresses =>
            val itV6 = CircularIterator(ipV6Addresses.toVector, threadSafe = true)
            _.set(LocalIpV6AddressAttributeName, itV6.next())
        }
      case singleIpV4Address :: Nil =>
        ipV6Addresses match {
          case Nil =>
            _.set(LocalIpV4AddressAttributeName, singleIpV4Address)
          case singleIpV6Address :: Nil =>
            _.set(LocalIpV4AddressAttributeName, singleIpV4Address)
              .set(LocalIpV6AddressAttributeName, singleIpV6Address)
          case _ =>
            val itV6 = CircularIterator(ipV6Addresses.toVector, threadSafe = true)
            _.set(LocalIpV4AddressAttributeName, singleIpV4Address)
              .set(LocalIpV6AddressAttributeName, itV6.next())
        }

      case _ =>
        val itV4 = CircularIterator(ipV4Addresses.toVector, threadSafe = true)
        ipV6Addresses match {
          case Nil =>
            _.set(LocalIpV4AddressAttributeName, itV4.next())
          case singleIpV6Address :: Nil =>
            _.set(LocalIpV4AddressAttributeName, itV4.next())
              .set(LocalIpV6AddressAttributeName, singleIpV6Address)
          case _ =>
            val itV6 = CircularIterator(ipV6Addresses.toVector, threadSafe = true)
            _.set(LocalIpV4AddressAttributeName, itV4.next())
              .set(LocalIpV6AddressAttributeName, itV6.next())
        }
    }
  }

  val localIpV4Address: Session => Option[InetAddress] =
    _.attributes.get(LocalIpV4AddressAttributeName).map(_.asInstanceOf[InetAddress])
  val localIpV6Address: Session => Option[InetAddress] =
    _.attributes.get(LocalIpV6AddressAttributeName).map(_.asInstanceOf[InetAddress])
}
