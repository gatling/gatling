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
package io.gatling.http

import io.gatling.http.protocol.Proxy

import org.asynchttpclient.Dsl
import org.asynchttpclient.Realm.AuthScheme
import org.asynchttpclient.proxy.ProxyServer

package object ahc {

  val NoCredentials = (null, null)

  implicit class ProxyConverter(val proxy: Proxy) extends AnyVal {

    def proxyServer: ProxyServer = {
      val realm = proxy.credentials.map(c => Dsl.realm(AuthScheme.BASIC, c.username, c.password).build)
      Dsl.proxyServer(proxy.host, proxy.port).setSecuredPort(proxy.securePort).setRealm(realm.orNull).build
    }
  }
}
