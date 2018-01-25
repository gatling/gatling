/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.ahc

import java.net.InetSocketAddress

import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.resolver.ExtendedDnsNameResolver

import akka.actor.ActorSystem

trait DnsNameResolverFactory {
  def newAsyncDnsNameResolver(dnsServers: Array[InetSocketAddress]): ExtendedDnsNameResolver
}

class DefaultDnsNameResolverFactory(system: ActorSystem, configuration: GatlingConfiguration) extends NettyFactory(system) with DnsNameResolverFactory {

  private val executor = newEventLoopGroup("gatling-dns-thread")

  override def newAsyncDnsNameResolver(dnsServers: Array[InetSocketAddress]): ExtendedDnsNameResolver =
    new ExtendedDnsNameResolver(
      executor.next(),
      configuration.http.dns.queryTimeout,
      configuration.http.dns.maxQueriesPerResolve,
      dnsServers
    )
}
