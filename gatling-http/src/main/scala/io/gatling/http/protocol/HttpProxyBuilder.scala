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

package io.gatling.http.protocol

import io.gatling.commons.model.Credentials

import com.softwaremill.quicklens._
import org.asynchttpclient.proxy.ProxyType._

object HttpProxyBuilder {

  def apply(host: String, port: Int): HttpProxyBuilder = new HttpProxyBuilder(Proxy(host, port, port, HTTP))

  implicit def toProxy(proxyBuilder: HttpProxyBuilder): Proxy = proxyBuilder.proxy
}

case class HttpProxyBuilder(proxy: Proxy) {

  def http: HttpProxyBuilder =
    this.modify(_.proxy.proxyType).setTo(HTTP)

  def socks4: HttpProxyBuilder =
    this.modify(_.proxy.proxyType).setTo(SOCKS_V4)

  def socks5: HttpProxyBuilder =
    this.modify(_.proxy.proxyType).setTo(SOCKS_V5)

  def httpsPort(port: Int): HttpProxyBuilder =
    this.modify(_.proxy.securePort).setTo(port)

  def credentials(username: String, password: String): HttpProxyBuilder =
    this.modify(_.proxy.credentials).setTo(Some(Credentials(username, password)))
}
