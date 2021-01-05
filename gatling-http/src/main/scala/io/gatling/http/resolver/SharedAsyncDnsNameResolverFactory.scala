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
import java.net.{ InetAddress, InetSocketAddress }
import java.util.concurrent.ConcurrentHashMap

import io.gatling.http.client.HttpListener
import io.gatling.http.client.resolver.InetAddressNameResolver
import io.gatling.http.engine.HttpEngine

import akka.actor.ActorSystem
import io.netty.channel.EventLoop
import io.netty.util.concurrent.{ Future, Promise }

object SharedAsyncDnsNameResolverFactory {

  def apply(httpEngine: HttpEngine, dnsServers: Array[InetSocketAddress], actorSystem: ActorSystem): EventLoop => InetAddressNameResolver = {
    // create shared name resolvers for all the users with this protocol
    val sharedResolverCache = new ConcurrentHashMap[EventLoop, InetAddressNameResolver]
    // perform close on system shutdown instead of virtual user termination as it's shared
    actorSystem.registerOnTermination(() => sharedResolverCache.values().forEach(_.close()))

    val inProgressResolutions = new ConcurrentHashMap[String, Promise[ju.List[InetAddress]]]

    val computer: ju.function.Function[EventLoop, InetAddressNameResolver] =
      el => {
        val actualResolver = httpEngine.newAsyncDnsNameResolver(el, dnsServers)
        new InflightInetAddressNameResolver(actualResolver, inProgressResolutions)
      }

    eventLoop => sharedResolverCache.computeIfAbsent(eventLoop, computer)
  }
}

class InflightInetAddressNameResolver(wrapped: InetAddressNameResolver, inProgressResolutions: ConcurrentHashMap[String, Promise[ju.List[InetAddress]]])
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

  private def transferResult(src: Future[ju.List[InetAddress]], dst: Promise[ju.List[InetAddress]]): Unit = {
    if (src.isSuccess) {
      dst.trySuccess(src.getNow)
    } else {
      dst.tryFailure(src.cause)
    }
  }

  // noop as shared
  override def close(): Unit = {}
}
