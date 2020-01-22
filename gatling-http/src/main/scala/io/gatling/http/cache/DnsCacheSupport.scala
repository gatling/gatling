/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import io.gatling.core.CoreComponents
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.client.HttpListener
import io.gatling.http.client.resolver.InetAddressNameResolver
import io.gatling.http.engine.HttpEngine
import io.gatling.http.protocol.{ AsyncDnsNameResolution, DnsNameResolution, HttpProtocol, JavaDnsNameResolution }
import io.gatling.http.resolver.{ AliasesAwareNameResolver, ShuffleJdkNameResolver }
import io.gatling.http.util.HttpTypeCaster

import io.netty.channel.EventLoop
import io.netty.util.concurrent.{ Future, Promise }

private object DnsCacheSupport {

  val DnsNameResolverAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.dns"
}

private class NoopCloseNameResolver(wrapped: InetAddressNameResolver) extends InetAddressNameResolver {

  override def resolveAll(inetHost: String, promise: Promise[JList[InetAddress]], listener: HttpListener): Future[JList[InetAddress]] =
    wrapped.resolveAll(inetHost, promise, listener)

  override def close(): Unit = {}
}

private[http] trait DnsCacheSupport {

  import DnsCacheSupport._

  def coreComponents: CoreComponents

  private def newNameResolver(
      eventLoop: => EventLoop,
      dnsNameResolution: DnsNameResolution,
      hostNameAliases: Map[String, InetAddress],
      httpEngine: HttpEngine
  ): InetAddressNameResolver = {
    val resolver: InetAddressNameResolver =
      coreComponents.configuration.resolve(
        // [fl]
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
          case JavaDnsNameResolution              => ShuffleJdkNameResolver.Instance
          case AsyncDnsNameResolution(dnsServers) => httpEngine.newAsyncDnsNameResolver(eventLoop, dnsServers)
        }
      )

    if (hostNameAliases.isEmpty) {
      resolver
    } else {
      new AliasesAwareNameResolver(hostNameAliases, resolver)
    }
  }

  def setNameResolver(httpProtocol: HttpProtocol, httpEngine: HttpEngine): Session => Session = {

    import httpProtocol.dnsPart._

    if (perUserNameResolution) { session =>
      session.set(DnsNameResolverAttributeName, newNameResolver(session.eventLoop, dnsNameResolution, hostNameAliases, httpEngine))

    } else {
      // create shared name resolver for all the users with this protocol
      val nameResolver = newNameResolver(coreComponents.eventLoopGroup.next(), dnsNameResolution, hostNameAliases, httpEngine)
      coreComponents.actorSystem.registerOnTermination(() => nameResolver.close())

      // perform close on system shutdown instead of virtual user termination as its shared
      val noopCloseNameResolver = new NoopCloseNameResolver(nameResolver)

      _.set(DnsNameResolverAttributeName, noopCloseNameResolver)
    }
  }

  def nameResolver(session: Session): Option[InetAddressNameResolver] = {
    // import optimized TypeCaster
    import HttpTypeCaster._
    session(DnsNameResolverAttributeName).asOption[InetAddressNameResolver]
  }
}
