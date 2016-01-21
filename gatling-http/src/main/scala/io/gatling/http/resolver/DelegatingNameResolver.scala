/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import java.net.InetAddress
import java.util.{ List => JList }

import io.netty.resolver.NameResolver
import io.netty.resolver.dns.DnsCache
import io.netty.util.concurrent.{ Future, Promise }

case class DelegatingNameResolver(resolver: ExtendedDnsNameResolver, cache: DnsCache)
    extends NameResolver[InetAddress] {

  override def resolve(inetHost: String): Future[InetAddress] =
    resolve(inetHost, resolver.executor.newPromise[InetAddress])

  override def resolve(inetHost: String, promise: Promise[InetAddress]): Future[InetAddress] = {
    resolver.doResolve(inetHost, promise, cache)
    promise
  }

  override def resolveAll(inetHost: String): Future[JList[InetAddress]] =
    resolveAll(inetHost, resolver.executor.newPromise[JList[InetAddress]])

  override def resolveAll(inetHost: String, promise: Promise[JList[InetAddress]]): Future[JList[InetAddress]] = {
    resolver.doResolveAll(inetHost, promise, cache)
    promise
  }

  override def close(): Unit = cache.clear()
}
