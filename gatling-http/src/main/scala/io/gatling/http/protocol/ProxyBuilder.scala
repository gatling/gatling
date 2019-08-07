/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

  def apply(host: String, port: Int): ProxyBuilder = new ProxyBuilder(Proxy(host, port, port, HttpProxy))

  implicit def toProxy(proxyBuilder: ProxyBuilder): Proxy = proxyBuilder.proxy
}

final case class ProxyBuilder(proxy: Proxy) {

  def http: ProxyBuilder =
    this.modify(_.proxy.proxyType).setTo(HttpProxy)

  def socks4: ProxyBuilder =
    this.modify(_.proxy.proxyType).setTo(Socks4Proxy)

  def socks5: ProxyBuilder =
    this.modify(_.proxy.proxyType).setTo(Socks5Proxy)

  def httpsPort(port: Int): ProxyBuilder =
    this.modify(_.proxy.securePort).setTo(port)

  def credentials(username: String, password: String): ProxyBuilder =
    this.modify(_.proxy.credentials).setTo(Some(Credentials(username, password)))
}
