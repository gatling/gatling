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

package io.gatling.http.util

import java.net._

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import com.typesafe.scalalogging.LazyLogging
import io.netty.util.NetUtil

object InetAddresses extends LazyLogging {

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
}
