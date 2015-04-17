/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.util

import java.net.{ UnknownHostException, InetAddress }

import org.xbill.DNS.Address._
import org.xbill.DNS._

object DnsHelper {

  @throws(classOf[UnknownHostException])
  def getAddressByName(name: String): InetAddress =
    toByteArray(name, IPv4) match {
      case null =>
        toByteArray(name, IPv6) match {
          case null =>
            val address = lookupHostNameAddress(name)
            InetAddress.getByAddress(name, address.getAddress)

          case ipV6Bytes => InetAddress.getByAddress(name, ipV6Bytes)
        }

      case ipV4Bytes => InetAddress.getByAddress(name, ipV4Bytes)
    }

  @throws(classOf[UnknownHostException])
  private def lookupHostNameAddress(name: String): InetAddress =
    try {
      val lookup: Lookup = new Lookup(name, Type.A)
      lookup.run match {
        case null =>
          if (lookup.getResult == Lookup.TYPE_NOT_FOUND) {
            new Lookup(name, Type.AAAA).run match {
              case null => throw new UnknownHostException("unknown host")
              case aaaa => aaaa(0).asInstanceOf[AAAARecord].getAddress
            }
          } else {
            throw new UnknownHostException("unknown host")
          }

        case a => a(0).asInstanceOf[ARecord].getAddress
      }
    } catch {
      case e: TextParseException => throw new UnknownHostException("invalid name")
    }
}
