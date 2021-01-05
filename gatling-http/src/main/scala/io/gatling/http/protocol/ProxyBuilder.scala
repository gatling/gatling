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

import com.softwaremill.quicklens._

object ProxyBuilder {

  def apply(host: String, port: Int): ProxyBuilder = new ProxyBuilder(Proxy(host, port, port, HttpProxy, None))

  implicit def toProxy(proxyBuilder: ProxyBuilder): Proxy = proxyBuilder.proxy
}

final class ProxyBuilder(val proxy: Proxy) {

  def http: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(HttpProxy))

  def socks4: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(Socks4Proxy))

  def socks5: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(Socks5Proxy))

  def httpsPort(port: Int): ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.securePort).setTo(port))

  def credentials(username: String, password: String): ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.credentials).setTo(Some(Credentials(username, password))))
}
