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

package io.gatling.http.resolver

import java.{ util => ju }
import java.net.InetAddress

import io.gatling.http.client.HttpListener
import io.gatling.http.client.resolver.InetAddressNameResolver
import io.gatling.http.util.{ InetAddresses, Lists }

import io.netty.channel.EventLoop
import io.netty.util.NetUtil
import io.netty.util.concurrent.{ Future, Promise }

private[http] class ShufflingNameResolver(wrapped: InetAddressNameResolver, eventLoop: EventLoop) extends InetAddressNameResolver {

  // no need for a CHM as will always be called from the v.u.'s EventLoop
  private val cache = new ju.HashMap[String, ju.List[InetAddress]](3)

  override def resolveAll(inetHost: String, promise: Promise[ju.List[InetAddress]], listener: HttpListener): Future[ju.List[InetAddress]] = {
    wrapped
      .resolveAll(inetHost, eventLoop.newPromise[ju.List[InetAddress]], listener)
      .addListener((future: Future[ju.List[InetAddress]]) =>
        if (future.isSuccess) {
          val rawAddresses = future.getNow
          if (rawAddresses.size == 1) {
            // don't bother checking equality or shuffling, directly clear cache and return
            cache.remove(inetHost)
            promise.setSuccess(rawAddresses)
          } else {
            val cachedAddresses = cache.get(inetHost)

            if (cachedAddresses != null && Lists.isSameSetAssumingNoDuplicate(cachedAddresses, rawAddresses)) {
              promise.setSuccess(cachedAddresses)
            } else {
              val shuffledAddresses = InetAddresses.shuffleInetAddresses(rawAddresses, NetUtil.isIpV4StackPreferred, NetUtil.isIpV6AddressesPreferred)
              cache.put(inetHost, shuffledAddresses)
              promise.setSuccess(shuffledAddresses)
            }
          }
        } else {
          promise.setFailure(future.cause)
        }
      )

    promise
  }

  override def close(): Unit = wrapped.close()
}
