/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import java.net.{ Inet4Address, Inet6Address, InetAddress, InetSocketAddress }

import scala.collection.immutable.ArraySeq

import io.gatling.commons.util.CircularIterator
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.client.LocalAddresses
import io.gatling.http.protocol.HttpProtocol

private[http] object LocalAddressSupport {
  private val LocalAddressesAttributeName: String = SessionPrivateAttributes.generatePrivateAttribute("http.cache.localAddresses")

  private def inetSocketAddressesIterator(inetAddresses: List[InetAddress]): Iterator[InetSocketAddress] = {
    val inetSocketAddressesWithRandomLocalPort = inetAddresses.map(new InetSocketAddress(_, 0))
    inetSocketAddressesWithRandomLocalPort match {
      case Nil           => Iterator.continually(null)
      case single :: Nil => Iterator.continually(single)
      case _             => CircularIterator(ArraySeq.from(inetSocketAddressesWithRandomLocalPort), threadSafe = true)
    }
  }

  def setLocalAddresses(httpProtocol: HttpProtocol): Session => Session = {
    val allLocalAddresses = httpProtocol.enginePart.localAddresses

    if (allLocalAddresses.isEmpty) {
      Session.Identity
    } else {
      val allLinkLocalIpV4Addresses = allLocalAddresses
        .filter {
          case addr: Inet4Address => addr.isLinkLocalAddress
          case _                  => false
        }

      val allSiteLocalIpV4Addresses = allLocalAddresses
        .filter {
          case addr: Inet4Address => addr.isSiteLocalAddress
          case _                  => false
        }

      val allPublicIpV4Addresses = allLocalAddresses
        .filter {
          case addr: Inet4Address => !addr.isLinkLocalAddress && !addr.isSiteLocalAddress
          case _                  => false
        }

      val allLinkLocalIpV6Addresses = allLocalAddresses
        .filter {
          case addr: Inet6Address => addr.isLinkLocalAddress
          case _                  => false
        }

      val allSiteLocalIpV6Addresses = allLocalAddresses
        .filter {
          case addr: Inet6Address => addr.isSiteLocalAddress
          case _                  => false
        }

      val allPublicIpV6Addresses = allLocalAddresses
        .filter {
          case addr: Inet6Address => !addr.isLinkLocalAddress && !addr.isSiteLocalAddress
          case _                  => false
        }

      val allLinkLocalIpV4AddressesIt = inetSocketAddressesIterator(allLinkLocalIpV4Addresses)
      val allSiteLocalIpV4AddressesIt = inetSocketAddressesIterator(allSiteLocalIpV4Addresses)
      // WARN: if we don't have any public local IPv4 and try to connect to a IPv4 destination, we can hope we have a NAT64 to do the translation
      val allPublicIpV4AddressesIt = inetSocketAddressesIterator(if (allPublicIpV4Addresses.nonEmpty) allPublicIpV4Addresses else allPublicIpV6Addresses)
      val allLinkLocalIpV6AddressesIt = inetSocketAddressesIterator(allLinkLocalIpV6Addresses)
      val allSiteLocalIpV6AddressesIt = inetSocketAddressesIterator(allSiteLocalIpV6Addresses)
      val allPublicIpV6AddressesIt = inetSocketAddressesIterator(allPublicIpV6Addresses)

      _.set(
        LocalAddressesAttributeName,
        new LocalAddresses(
          allLinkLocalIpV4AddressesIt.next(),
          allSiteLocalIpV4AddressesIt.next(),
          allPublicIpV4AddressesIt.next(),
          allLinkLocalIpV6AddressesIt.next(),
          allSiteLocalIpV6AddressesIt.next(),
          allPublicIpV6AddressesIt.next()
        )
      )
    }
  }

  def localAddresses(session: Session): Option[LocalAddresses] =
    session.attributes.get(LocalAddressesAttributeName).map(_.asInstanceOf[LocalAddresses])
}
