/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.resolver.ExtendedDnsNameResolver

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory

object DnsNameResolverFactory {
  def apply(coreComponents: CoreComponents): DnsNameResolverFactory = {
    val eventLoopGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("gatling-dns"))
    coreComponents.actorSystem.registerOnTermination(eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS))
    new DnsNameResolverFactory(eventLoopGroup, coreComponents.configuration)
  }
}

class DnsNameResolverFactory(eventLoopGroup: EventLoopGroup, configuration: GatlingConfiguration) extends EventLoopGroups {

  def newAsyncDnsNameResolver(dnsServers: Array[InetSocketAddress]): ExtendedDnsNameResolver =
    new ExtendedDnsNameResolver(
      eventLoopGroup.next(),
      configuration.http.dns.queryTimeout.toMillis.toInt,
      configuration.http.dns.maxQueriesPerResolve,
      dnsServers
    )
}
