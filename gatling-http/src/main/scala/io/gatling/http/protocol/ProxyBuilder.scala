/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import io.gatling.core.session.{ Expression, ExpressionSuccessWrapper }

import com.softwaremill.quicklens._

object ProxyBuilder {

  def apply(host: Expression[String], port: Expression[Int]): ProxyBuilder = new ProxyBuilder(Proxy(host, port, port, HttpProxy, None))

  implicit def toProxy(proxyBuilder: ProxyBuilder): Expression[Proxy] = proxyBuilder.proxy.expressionSuccess
}

final class ProxyBuilder(val proxy: Proxy) {

  def http: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(HttpProxy))

  def socks4: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(Socks4Proxy))

  def socks5: ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.proxyType).setTo(Socks5Proxy))

  def httpsPort(port: Expression[Int]): ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.securePort).setTo(port))

  def credentials(username: Expression[String], password: Expression[String]): ProxyBuilder =
    new ProxyBuilder(proxy.modify(_.credentials).setTo(Some(ProxyCredentials(username, password).expressionSuccess)))
}
