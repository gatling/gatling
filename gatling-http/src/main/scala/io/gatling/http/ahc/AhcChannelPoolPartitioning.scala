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

import java.util.function.Predicate

import io.gatling.core.session.Session

import org.asynchttpclient.channel.ChannelPoolPartitioning
import org.asynchttpclient.proxy.{ ProxyServer, ProxyType }
import org.asynchttpclient.uri.Uri

case class ChannelPoolKey(userId: Long, remoteKey: RemoteKey)

sealed trait RemoteKey

private[ahc] case class NoVirtualHostNoProxyKey(scheme: String, host: String, port: Int) extends RemoteKey

private[ahc] case class NoVirtualHostKey(scheme: String, host: String, port: Int, proxyHost: String, proxyPort: Int, proxyType: ProxyType) extends RemoteKey

private[ahc] case class NoProxyHostKey(scheme: String, host: String, port: Int, virtualHost: String) extends RemoteKey

private[ahc] case class FullKey(scheme: String, host: String, port: Int, virtualHost: String, proxyHost: String, proxyPort: Int, proxyType: ProxyType) extends RemoteKey

object AhcChannelPoolPartitioning {
  def flushPredicate(session: Session): Predicate[Object] = {
    case ChannelPoolKey(session.userId, _) => true
    case _                                 => false
  }
}

class AhcChannelPoolPartitioning(session: Session) extends ChannelPoolPartitioning {
  override def getPartitionKey(uri: Uri, virtualHost: String, proxyServer: ProxyServer): ChannelPoolKey = {
    val key =
      if (proxyServer == null) {
        if (virtualHost == null) {
          NoVirtualHostNoProxyKey(uri.getScheme, uri.getHost, uri.getExplicitPort)
        } else {
          NoProxyHostKey(uri.getScheme, uri.getHost, uri.getExplicitPort, virtualHost)
        }
      } else {
        val proxyPort = if (uri.isSecured && proxyServer.getProxyType == ProxyType.HTTP) proxyServer.getSecuredPort else proxyServer.getPort

        if (virtualHost == null) {
          NoVirtualHostKey(uri.getScheme, uri.getHost, uri.getExplicitPort, proxyServer.getHost, proxyPort, proxyServer.getProxyType)
        } else {
          FullKey(uri.getScheme, uri.getHost, uri.getExplicitPort, virtualHost, proxyServer.getHost, proxyPort, proxyServer.getProxyType)
        }
      }
    ChannelPoolKey(session.userId, key)
  }
}
