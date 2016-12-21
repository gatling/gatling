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
package io.netty.resolver.dns

import io.netty.channel.socket.InternetProtocolFamily

object NettyDnsConstants {

  // FIXME to be removed when upgrading to Netty 4.1
  val IsIpV4StackPreferred = java.lang.Boolean.getBoolean("java.net.preferIPv4Stack")
  val IsIpV6AddressesPreferred = java.lang.Boolean.getBoolean("java.net.preferIPv6Addresses")

  val DefaultResolveAddressTypes: Array[InternetProtocolFamily] =
    if (IsIpV4StackPreferred) {
      Array(InternetProtocolFamily.IPv4)
    } else if (IsIpV6AddressesPreferred) {
      Array(InternetProtocolFamily.IPv6, InternetProtocolFamily.IPv4)
    } else {
      Array(InternetProtocolFamily.IPv4, InternetProtocolFamily.IPv6)
    }

  val DefaultSearchDomain = DnsNameResolver.DEFAULT_SEACH_DOMAINS
}
