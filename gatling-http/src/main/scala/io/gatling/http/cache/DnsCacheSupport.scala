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
package io.gatling.http.cache

import java.net.InetAddress

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.resolver.{ AliasesAwareNameResolver, ShuffleJdkNameResolver }
import io.gatling.http.util.HttpTypeHelper

import io.netty.resolver.{ DefaultNameResolver, NameResolver }
import io.netty.util.concurrent.ImmediateEventExecutor

object DnsCacheSupport {

  val JavaDnsCacheEternal = sys.props.get("sun.net.inetaddr.ttl").getOrElse(-1) == -1

  val JavaNameResolver = new DefaultNameResolver(ImmediateEventExecutor.INSTANCE)

  val DnsCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.dns"
}

trait DnsCacheSupport {

  import DnsCacheSupport._

  def configuration: GatlingConfiguration

  def setNameResolver(httpProtocol: HttpProtocol, httpEngine: HttpEngine): Session => Session = {

    val hostAliases = httpProtocol.enginePart.hostNameAliases
    if (hostAliases.isEmpty) {
      if (httpProtocol.enginePart.perUserNameResolution) {
        // use per user resolver
        _.set(DnsCacheAttributeName, httpEngine.newDnsResolver)

      } else if (JavaDnsCacheEternal) {
        // mitigate missing round robin
        _.set(DnsCacheAttributeName, new ShuffleJdkNameResolver)

      } else {
        // user tuned Java behavior, let him have the standard behavior
        _.set(DnsCacheAttributeName, JavaNameResolver)
      }
    } else {
      if (httpProtocol.enginePart.perUserNameResolution) {
        // use per user resolver
        _.set(DnsCacheAttributeName, new AliasesAwareNameResolver(hostAliases, httpEngine.newDnsResolver))

      } else if (JavaDnsCacheEternal) {
        // mitigate missing round robin
        _.set(DnsCacheAttributeName, new AliasesAwareNameResolver(hostAliases, new ShuffleJdkNameResolver))

      } else {
        // user tuned Java behavior, let him have the standard behavior
        _.set(DnsCacheAttributeName, new AliasesAwareNameResolver(hostAliases, JavaNameResolver))
      }
    }
  }

  val nameResolver: (Session => Option[NameResolver[InetAddress]]) = {
    // import optimized TypeCaster
    import HttpTypeHelper._
    _(DnsCacheAttributeName).asOption[NameResolver[InetAddress]]
  }
}
