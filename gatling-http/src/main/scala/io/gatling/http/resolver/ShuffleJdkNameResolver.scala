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

package io.gatling.http.resolver

import java.{ util => ju }
import java.net.{ Inet4Address, InetAddress, UnknownHostException }
import java.util.concurrent.ThreadLocalRandom

import io.gatling.http.client.HttpListener
import io.gatling.http.client.resolver.InetAddressNameResolver

import io.netty.util.NetUtil
import io.netty.util.concurrent.{ Future, Promise }

private[http] object ShuffleJdkNameResolver {
  val Instance = new ShuffleJdkNameResolver

  @SuppressWarnings(Array("org.wartremover.warts.IsInstanceOf", "org.wartremover.warts.AnyVal"))
  def shuffleInetAddresses(originalAddresses: Array[InetAddress], isIpV4StackPreferred: Boolean, isIpV6AddressesPreferred: Boolean): ju.List[InetAddress] =
    if (originalAddresses.length == 1) {
      ju.Collections.singletonList(originalAddresses(0))
    } else {
      var ipV4: ju.List[InetAddress] = null
      var ipV6: ju.List[InetAddress] = null
      originalAddresses.foreach { inetAddress =>
        if (inetAddress.isInstanceOf[Inet4Address]) {
          if (ipV4 == null) {
            ipV4 = new ju.ArrayList[InetAddress](originalAddresses.length)
          }
          ipV4.add(inetAddress)
        } else if (!isIpV4StackPreferred) {
          if (ipV6 == null) {
            ipV6 = new ju.ArrayList[InetAddress](originalAddresses.length)
          }
          ipV6.add(inetAddress)
        }
      }

      val higher = if (isIpV6AddressesPreferred) ipV6 else ipV4
      val lower = if (isIpV6AddressesPreferred) ipV4 else ipV6

      val random = ThreadLocalRandom.current()
      if (higher != null) {
        ju.Collections.shuffle(higher, random)
        if (lower != null) {
          ju.Collections.shuffle(lower, random)
          higher.addAll(lower)
        }
        higher
      } else {
        ju.Collections.shuffle(lower, random)
        lower
      }
    }
}

private[http] class ShuffleJdkNameResolver extends InetAddressNameResolver {

  protected def resolveAll0(inetHost: String, promise: Promise[ju.List[InetAddress]]): Unit =
    try {
      val originalAddresses: Array[InetAddress] = InetAddress.getAllByName(inetHost)
      val addresses = ShuffleJdkNameResolver.shuffleInetAddresses(originalAddresses, NetUtil.isIpV4StackPreferred, NetUtil.isIpV6AddressesPreferred)
      promise.setSuccess(addresses)
    } catch {
      case e: UnknownHostException => promise.setFailure(e)
    }

  override def resolveAll(inetHost: String, promise: Promise[ju.List[InetAddress]], listener: HttpListener): Future[ju.List[InetAddress]] = {
    resolveAll0(inetHost, promise)
    promise
  }

  override def close(): Unit = {}
}
