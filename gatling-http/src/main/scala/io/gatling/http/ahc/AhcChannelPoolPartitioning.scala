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
package io.gatling.http.ahc

import io.gatling.core.session.Session

import org.asynchttpclient.channel.{ ChannelPoolPartitioning, ChannelPoolPartitionSelector }
import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.uri.Uri

case class ChannelPoolKey(userId: Long, remoteKey: RemoteKey)

sealed trait RemoteKey
case class RemoteServerKey(scheme: String, hostname: String, port: Int) extends RemoteKey
case class VirtualHostKey(virtualHost: String) extends RemoteKey
case class ProxyServerKey(hostname: String, port: Int, secure: Boolean, targetHostKey: RemoteKey) extends RemoteKey

class AhcChannelPoolPartitioning(session: Session) extends ChannelPoolPartitioning {

  override def getPartitionKey(uri: Uri, virtualHost: String, proxyServer: ProxyServer): ChannelPoolKey = {

    val targetHostKey =
      if (virtualHost == null)
        RemoteServerKey(uri.getScheme, uri.getHost, uri.getExplicitPort)
      else
        VirtualHostKey(virtualHost)

    val remoteKey =
      if (proxyServer == null) {
        targetHostKey
      } else if (uri.isSecured) {
        new ProxyServerKey(proxyServer.getHost, proxyServer.getSecuredPort, true, targetHostKey)
      } else {
        new ProxyServerKey(proxyServer.getHost, proxyServer.getPort, false, targetHostKey)
      }

    ChannelPoolKey(session.userId, remoteKey)
  }
}

class AhcChannelPoolPartitionSelector(userId: Long) extends ChannelPoolPartitionSelector {

  override def select(partitionKey: Object): Boolean = partitionKey match {
    case ChannelPoolKey(`userId`, _) => true
    case _                           => false
  }
}
