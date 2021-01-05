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

package io.gatling.http.protocol

import io.gatling.commons.model.Credentials
import io.gatling.http.client.proxy._
import io.gatling.http.client.realm.BasicRealm

final case class Proxy(
    host: String,
    port: Int,
    securePort: Int,
    proxyType: ProxyType,
    credentials: Option[Credentials]
) {
  def proxyServer: ProxyServer = {
    def basicRealm: Option[BasicRealm] = credentials.map(c => new BasicRealm(c.username, c.password))

    proxyType match {
      case HttpProxy   => new HttpProxyServer(host, port, securePort, basicRealm.orNull)
      case Socks4Proxy => new Socks4ProxyServer(host, port, credentials.map(_.username).orNull)
      case Socks5Proxy => new Socks5ProxyServer(host, port, basicRealm.orNull)
    }
  }
}

sealed trait ProxyType
case object HttpProxy extends ProxyType
case object Socks4Proxy extends ProxyType
case object Socks5Proxy extends ProxyType
