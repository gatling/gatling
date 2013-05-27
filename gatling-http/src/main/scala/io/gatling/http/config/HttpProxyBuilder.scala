/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.config

import io.gatling.core.config.Credentials
import io.gatling.http.util.HttpHelper.buildProxy

class HttpProxyBuilder(protocolBuilder: HttpProtocolBuilder, host: String, port: Int, sslPort: Option[Int], credentials: Option[Credentials]) {

	def this(protocolBuilder: HttpProtocolBuilder, host: String, port: Int) = this(protocolBuilder, host, port, None, None)

	def httpsPort(sslPort: Int) = new HttpProxyBuilder(protocolBuilder, host, port, Some(sslPort), credentials)

	def credentials(username: String, password: String) = new HttpProxyBuilder(protocolBuilder, host, port, sslPort, Some(Credentials(username, password)))

	def toHttpProtocolBuilder = protocolBuilder.addProxies(buildProxy(host, port, credentials, false), sslPort.map(buildProxy(host, _, credentials, true)))
}