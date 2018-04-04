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

package io.gatling.http.cache

import java.net.InetAddress
import java.util.{ List => JList }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.engine.HttpEngine
import io.gatling.http.protocol.{ AsyncDnsNameResolution, DnsNameResolution, HttpProtocol, JavaDnsNameResolution }
import io.gatling.http.resolver.{ AliasesAwareNameResolver, CacheOverrideNameResolver, ShuffleJdkNameResolver }
import io.gatling.http.util.HttpTypeCaster

import io.netty.resolver.NameResolver
import io.netty.resolver.dns.DefaultDnsCache
import io.netty.util.concurrent.{ Future, Promise }

object DnsCacheSupport {

  val DnsNameResolverAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.dns"

  private def newNameResolver(
    dnsNameResolution: DnsNameResolution,
    hostNameAliases:   Map[String, InetAddress],
    httpEngine:        HttpEngine,
    configuration:     GatlingConfiguration
  ) =
    configuration.resolve(
      // [fl]
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      // [fl]
      dnsNameResolution match {
        case JavaDnsNameResolution =>
          val shuffleJdkNameResolver = new ShuffleJdkNameResolver
          if (hostNameAliases.isEmpty) {
            shuffleJdkNameResolver
          } else {
            new AliasesAwareNameResolver(hostNameAliases, shuffleJdkNameResolver)
          }

        case AsyncDnsNameResolution(dnsServers) =>
          if (hostNameAliases.isEmpty) {
            new CacheOverrideNameResolver(httpEngine.dnsNameResolverFactory.newAsyncDnsNameResolver(dnsServers), new DefaultDnsCache)
          } else {
            new AliasesAwareNameResolver(hostNameAliases, httpEngine.dnsNameResolverFactory.newAsyncDnsNameResolver(dnsServers))
          }
      }
    )
}

trait DnsCacheSupport {

  import DnsCacheSupport._

  def configuration: GatlingConfiguration

  def setNameResolver(httpProtocol: HttpProtocol, httpEngine: HttpEngine): Session => Session = {

    import httpProtocol.dnsPart._

    if (perUserNameResolution) {
      _.set(DnsNameResolverAttributeName, newNameResolver(dnsNameResolution, hostNameAliases, httpEngine, configuration))

    } else {
      // create shared name resolver for all the users with this protocol
      val nameResolver = newNameResolver(dnsNameResolution, hostNameAliases, httpEngine, configuration)
      httpEngine.coreComponents.system.registerOnTermination(() => nameResolver.close())

      // perform close on system shutdown instead of virtual user termination as its shared
      val noopCloseNameResolver = new NameResolver[InetAddress] {
        override def resolve(inetHost: String): Future[InetAddress] =
          nameResolver.resolve(inetHost)

        override def resolve(inetHost: String, promise: Promise[InetAddress]): Future[InetAddress] =
          nameResolver.resolve(inetHost, promise)

        override def resolveAll(inetHost: String): Future[JList[InetAddress]] =
          nameResolver.resolveAll(inetHost)

        override def resolveAll(inetHost: String, promise: Promise[JList[InetAddress]]): Future[JList[InetAddress]] =
          nameResolver.resolveAll(inetHost, promise)

        override def close(): Unit = {}
      }

      _.set(DnsNameResolverAttributeName, noopCloseNameResolver)
    }
  }

  def nameResolver(session: Session): Option[NameResolver[InetAddress]] = {
    // import optimized TypeCaster
    import HttpTypeCaster._
    session(DnsNameResolverAttributeName).asOption[NameResolver[InetAddress]]
  }
}
