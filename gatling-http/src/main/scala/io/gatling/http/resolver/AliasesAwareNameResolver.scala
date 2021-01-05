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
import io.gatling.http.util.InetAddresses

import io.netty.util.NetUtil
import io.netty.util.concurrent.{ Future, Promise }

private[http] object AliasesAwareNameResolver {
  def apply(aliases: Map[String, ju.List[InetAddress]], wrapped: InetAddressNameResolver): InetAddressNameResolver = {
    if (aliases.isEmpty) {
      wrapped
    } else {
      val shuffledAliases =
        aliases.view.mapValues(InetAddresses.shuffleInetAddresses(_, NetUtil.isIpV4StackPreferred, NetUtil.isIpV6AddressesPreferred)).to(Map)
      new AliasesAwareNameResolver(shuffledAliases, wrapped)
    }
  }
}

private class AliasesAwareNameResolver(aliases: Map[String, ju.List[InetAddress]], wrapped: InetAddressNameResolver) extends InetAddressNameResolver {

  override def resolveAll(inetHost: String, promise: Promise[ju.List[InetAddress]], listener: HttpListener): Future[ju.List[InetAddress]] =
    aliases.get(inetHost) match {
      case Some(addresses) => promise.setSuccess(addresses)
      case _               => wrapped.resolveAll(inetHost, promise, listener)
    }

  override def close(): Unit = wrapped.close()
}
