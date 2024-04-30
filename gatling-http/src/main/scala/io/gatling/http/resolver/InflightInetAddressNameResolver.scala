/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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
import java.util.concurrent.ConcurrentHashMap

import io.gatling.http.client.HttpListener
import io.gatling.http.client.resolver.InetAddressNameResolver

import io.netty.util.concurrent.{ Future, Promise }

final class InflightInetAddressNameResolver(wrapped: InetAddressNameResolver, inProgressResolutions: ConcurrentHashMap[String, Promise[ju.List[InetAddress]]])
    extends InetAddressNameResolver {
  override def resolveAll(inetHost: String, promise: Promise[ju.List[InetAddress]], listener: HttpListener): Future[ju.List[InetAddress]] = {
    val earlyPromise = inProgressResolutions.putIfAbsent(inetHost, promise)
    if (earlyPromise != null) {
      // name resolution for the specified inetHost is already in progress
      if (earlyPromise.isDone) {
        transferResult(earlyPromise, promise)
      } else {
        earlyPromise.addListener(transferResult(_, promise))
      }
    } else {
      try {
        wrapped.resolveAll(inetHost, promise, listener)
      } finally {
        if (promise.isDone) {
          inProgressResolutions.remove(inetHost)
        } else {
          promise.addListener((_: Future[ju.List[InetAddress]]) => inProgressResolutions.remove(inetHost))
        }
      }
    }

    promise
  }

  private def transferResult(src: Future[ju.List[InetAddress]], dst: Promise[ju.List[InetAddress]]): Unit =
    if (src.isSuccess) {
      dst.trySuccess(src.getNow)
    } else {
      dst.tryFailure(src.cause)
    }

  // noop as shared
  override def close(): Unit = {}
}
