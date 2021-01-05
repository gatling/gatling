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

package io.gatling.http.util

import java.{ util => ju }
import java.net._
import java.util.concurrent.ThreadLocalRandom

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

import com.typesafe.scalalogging.LazyLogging
import io.netty.util.NetUtil

private[http] object InetAddresses extends LazyLogging {

  private def isBindable(localAddress: InetAddress): Boolean = {
    val socket = new Socket
    val socketAddress = new InetSocketAddress(localAddress, 0)
    try {
      socket.bind(socketAddress)
      true
    } catch {
      case NonFatal(e) =>
        logger.debug(s"Address $localAddress is not bindable", e)
        false
    } finally {
      socket.close()
    }
  }

  private val AllLocalAddresses =
    for {
      networkInterface <- NetworkInterface.getNetworkInterfaces.asScala.toList
      inetAddress <- networkInterface.getInetAddresses.asScala
      if !inetAddress.isLoopbackAddress &&
        !inetAddress.isLinkLocalAddress &&
        !inetAddress.isMulticastAddress &&
        isBindable(inetAddress)
    } yield inetAddress

  val AllIpV4LocalAddresses: List[InetAddress] = AllLocalAddresses.filter(_.isInstanceOf[Inet4Address])
  val AllIpV6LocalAddresses: List[InetAddress] = if (NetUtil.isIpV4StackPreferred) Nil else AllLocalAddresses.filter(_.isInstanceOf[Inet6Address])

  @SuppressWarnings(Array("org.wartremover.warts.IsInstanceOf", "org.wartremover.warts.AnyVal"))
  def shuffleInetAddresses(originalAddresses: ju.List[InetAddress], isIpV4StackPreferred: Boolean, isIpV6AddressesPreferred: Boolean): ju.List[InetAddress] =
    if (originalAddresses.size() == 1) {
      originalAddresses
    } else {
      var ipV4: ju.List[InetAddress] = null
      var ipV6: ju.List[InetAddress] = null

      originalAddresses.asScala.foreach { inetAddress =>
        if (inetAddress.isInstanceOf[Inet4Address]) {
          if (ipV4 == null) {
            ipV4 = new ju.ArrayList[InetAddress](originalAddresses.size())
          }
          ipV4.add(inetAddress)
        } else if (!isIpV4StackPreferred) {
          if (ipV6 == null) {
            ipV6 = new ju.ArrayList[InetAddress](originalAddresses.size())
          }
          ipV6.add(inetAddress)
        }
      }

      val higher = if (isIpV6AddressesPreferred) ipV6 else ipV4
      val lower = if (isIpV6AddressesPreferred) ipV4 else ipV6

      val random = ThreadLocalRandom.current()
      if (higher != null) {
        shuffle(higher, random)
        if (lower != null) {
          shuffle(lower, random)
          higher.addAll(lower)
        }
        higher
      } else {
        shuffle(lower, random)
        lower
      }
    }

  private def shuffle(list: ju.List[InetAddress], random: ThreadLocalRandom): Unit =
    if (list.size() > 1) {
      ju.Collections.shuffle(list, random)
    }
}
