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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.resolver._
import io.gatling.http.util.HttpTypeCaster

import io.netty.resolver.dns.DefaultDnsCache
import io.netty.resolver.NameResolver
import org.asynchttpclient.RequestBuilderBase

object DnsCacheSupport {

  private val UseDefaultJavaEternalDnsCache = sys.props.get("sun.net.inetaddr.ttl").getOrElse(-1) == -1

  val DnsNameResolverAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.dns"
}

trait DnsCacheSupport {

  import DnsCacheSupport._

  def configuration: GatlingConfiguration

  def setNameResolver(httpProtocol: HttpProtocol, httpEngine: HttpEngine): Session => Session = {

    val hostAliases = httpProtocol.enginePart.hostNameAliases
    if (hostAliases.isEmpty) {
      if (httpProtocol.enginePart.perUserNameResolution) {
        // use per user resolver
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
          // [fl]
          _.set(DnsNameResolverAttributeName, new CacheOverrideNameResolver(httpEngine.ahcFactory.defaultDnsNameResolver, new DefaultDnsCache))
        )

      } else if (UseDefaultJavaEternalDnsCache) {
        // mitigate missing round robin
        _.set(DnsNameResolverAttributeName, new ShuffleJdkNameResolver)

      } else {
        // use AHC's default name resolution
        identity
      }
    } else {
      if (httpProtocol.enginePart.perUserNameResolution) {
        // use per user resolver
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
          // [fl]
          _.set(DnsNameResolverAttributeName, new AliasesAwareNameResolver(hostAliases, httpEngine.ahcFactory.defaultDnsNameResolver))
        )

      } else if (UseDefaultJavaEternalDnsCache) {
        // mitigate missing round robin
        _.set(DnsNameResolverAttributeName, new AliasesAwareNameResolver(hostAliases, new ShuffleJdkNameResolver))

      } else {
        // user tuned Java behavior, let him have the standard behavior
        _.set(DnsNameResolverAttributeName, new AliasesAwareNameResolver(hostAliases, RequestBuilderBase.DEFAULT_NAME_RESOLVER))
      }
    }
  }

  def nameResolver(session: Session): Option[NameResolver[InetAddress]] = {
    // import optimized TypeCaster
    import HttpTypeCaster._
    session(DnsNameResolverAttributeName).asOption[NameResolver[InetAddress]]
  }
}
