/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.uri.Uri

object RemoteKey {
  def apply(uri: Uri, virtualHost: String, proxyServer: ProxyServer): RemoteKey = {
    val targetHostKey =
      if (virtualHost == null) {
        RemoteServerKey(uri.getScheme, uri.getHost, uri.getExplicitPort)
      } else {
        VirtualHostKey(virtualHost)
      }

    if (proxyServer == null) {
      targetHostKey
    } else if (uri.isSecured) {
      ProxyServerKey(proxyServer.getHost, proxyServer.getSecuredPort, secure = true, targetHostKey)
    } else {
      ProxyServerKey(proxyServer.getHost, proxyServer.getPort, secure = false, targetHostKey)
    }
  }
}

sealed trait RemoteKey
case class RemoteServerKey(scheme: String, hostname: String, port: Int) extends RemoteKey
case class VirtualHostKey(virtualHost: String) extends RemoteKey
case class ProxyServerKey(hostname: String, port: Int, secure: Boolean, targetHostKey: RemoteKey) extends RemoteKey
