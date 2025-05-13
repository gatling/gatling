/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.core.session.Expression
import io.gatling.core.session.el.El
import io.gatling.http.util.HttpHelper._
import io.gatling.internal.quicklens._

object ProxyBuilder {
  def apply(host: String, port: Int): ProxyBuilder = new ProxyBuilder(Proxy(host, port, HttpProxy, None, Map.empty))

  implicit def toProxy(proxyBuilder: ProxyBuilder): Proxy = proxyBuilder.proxy
}

final class ProxyBuilder(val proxy: Proxy) {
  def http: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(HttpProxy))

  def https: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(HttpsProxy))

  def socks4: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(Socks4Proxy))

  def socks5: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(Socks5Proxy))

  def credentials(username: Expression[String], password: Expression[String]): ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.basicRealm).setTo(Some(buildBasicAuthRealm(username, password))))

  def connectHeader(name: CharSequence, value: Expression[String]): ProxyBuilder = {
    require(proxy.proxyType == HttpProxy || proxy.proxyType == HttpsProxy, "Proxy CONNECT headers are only supported on HTTP(S) proxies")
    new ProxyBuilder(proxy.modify(_.connectHeaders)(_ + (name -> value)))
  }

  def connectHeaders(headers: Map[_ <: CharSequence, String]): ProxyBuilder = {
    require(proxy.proxyType == HttpProxy || proxy.proxyType == HttpsProxy, "Proxy CONNECT headers are only supported on HTTP(S) proxies")
    new ProxyBuilder(proxy.modify(_.connectHeaders)(_ ++ headers.view.mapValues(_.el[String])))
  }
}
