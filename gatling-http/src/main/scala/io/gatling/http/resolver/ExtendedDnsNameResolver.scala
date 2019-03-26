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

package io.gatling.http.resolver

import java.net.{ InetAddress, InetSocketAddress }
import java.util.{ List => JList }

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.ChannelFactory
import io.netty.channel.EventLoop
import io.netty.channel.socket.DatagramChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.dns.DnsRecord
import io.netty.resolver.{ HostsFileEntriesResolver, ResolvedAddressTypes }
import io.netty.resolver.dns._
import io.netty.util.NetUtil
import io.netty.util.concurrent.Promise

private[resolver] object ExtendedDnsNameResolver extends StrictLogging {

  private val DebugEnabled = logger.underlying.isDebugEnabled

  private val NioDatagramChannelFactory = new ChannelFactory[DatagramChannel] {
    override def newChannel(): DatagramChannel = new NioDatagramChannel
  }
}

/**
 * DnsNameResolver whose sole purpose is to publicly expose the doResolve and executor methods that are protected
 */
private[http] class ExtendedDnsNameResolver(
    val eventLoop:        EventLoop,
    queryTimeout:         Int,
    maxQueriesPerResolve: Int,
    dnsServers:           Array[InetSocketAddress]
)
  extends DnsNameResolver(
    eventLoop, // eventLoop
    ExtendedDnsNameResolver.NioDatagramChannelFactory, // channelFactory
    NoopDnsCache.INSTANCE, // resolveCache
    NoopAuthoritativeDnsServerCache.INSTANCE, // authoritativeDnsServerCache
    NoopDnsQueryLifecycleObserverFactory.INSTANCE, // dnsQueryLifecycleObserverFactory
    queryTimeout, // queryTimeoutMillis
    null, // resolvedAddressTypes
    true, // recursionDesired
    maxQueriesPerResolve, // maxQueriesPerResolve
    ExtendedDnsNameResolver.DebugEnabled, // traceEnabled
    4096, // maxPayloadSize
    true, // optResourceEnabled
    HostsFileEntriesResolver.DEFAULT, // hostsFileEntriesResolver
    if (dnsServers.length == 0) DnsServerAddressStreamProviders.platformDefault else new SequentialDnsServerAddressStreamProvider(dnsServers: _*), // dnsServerAddressStreamProvider
    null, // searchDomains
    -1, // ndots
    true // decodeIdn
  ) {
  override def doResolveAll(inetHost: String, additionals: Array[DnsRecord], promise: Promise[JList[InetAddress]], resolveCache: DnsCache): Unit =
    super.doResolveAll(inetHost, additionals, promise, resolveCache)
}
