/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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
import io.gatling.http.client.realm.BasicRealm

final case class Proxy(
    host: String,
    port: Int,
    proxyType: ProxyType,
    basicRealm: Option[Expression[BasicRealm]],
    connectHeaders: Map[CharSequence, Expression[String]]
)

sealed trait ProxyType
case object HttpProxy extends ProxyType
case object HttpsProxy extends ProxyType
case object Socks4Proxy extends ProxyType
case object Socks5Proxy extends ProxyType
