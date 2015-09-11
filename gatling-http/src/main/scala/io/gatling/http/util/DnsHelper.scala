/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

import org.asynchttpclient.channel.NameResolution
import org.xbill.DNS.Address._
import org.xbill.DNS._

object DnsHelper {

  @throws(classOf[UnknownHostException])
  def getAddressesByName(name: String): Array[NameResolution] =
    toByteArray(name, IPv4) match {
      case null => toByteArray(name, IPv6) match {
        case null      => lookupHostNameAddresses(name)
        case ipV6Bytes => Array(new NameResolution(InetAddress.getByAddress(name, ipV6Bytes), Long.MaxValue))
      }

      case ipV4Bytes => Array(new NameResolution(InetAddress.getByAddress(name, ipV4Bytes), Long.MaxValue))
    }

  private def newNameResolution(name: String, address: InetAddress, ttl: Long): NameResolution =
    new NameResolution(InetAddress.getByAddress(name, address.getAddress), System.currentTimeMillis + ttl * 1000L)

  @throws(classOf[UnknownHostException])
  private def lookupHostNameAddresses(name: String): Array[NameResolution] =
    try {
      val lookup: Lookup = new Lookup(name, Type.A)
      lookup.run match {
        case null =>
          if (lookup.getResult == Lookup.TYPE_NOT_FOUND) {
            new Lookup(name, Type.AAAA).run match {
              case null => throw new UnknownHostException("unknown host")
              case rec  => rec.collect { case aaaa: AAAARecord => newNameResolution(name, aaaa.getAddress, aaaa.getTTL) }
            }
          } else {
            throw new UnknownHostException("unknown host")
          }

        case rec => rec.collect { case a: ARecord => newNameResolution(name, a.getAddress, a.getTTL) }
      }
    } catch {
      case e: TextParseException => throw new UnknownHostException("invalid name")
    }
}
