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
package io.gatling.recorder.scenario.template

import io.gatling.core.util.StringHelper.eol
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.scenario.ProtocolElement.baseHeaders

import com.dongxiguo.fastring.Fastring.Implicits._

object ProtocolTemplate {

	val indent = "\t" * 2

	def render(baseUrl: String, headers: Map[String, String]) = {

		def renderProxy = {

			def renderSslPort = configuration.proxy.outgoing.sslPort.map(proxySslPort => s".httpsPort($proxySslPort)").getOrElse("")

			def renderCredentials = {
				val credentials = for {
					proxyUsername <- configuration.proxy.outgoing.username
					proxyPassword <- configuration.proxy.outgoing.password
				} yield s"""$eol$indent.credentials("$proxyUsername","$proxyPassword")"""
				credentials.getOrElse("")
			}

			val protocol = for {
				proxyHost <- configuration.proxy.outgoing.host
				proxyPort <- configuration.proxy.outgoing.port
			} yield s"""$eol$indent.proxy(Proxy("$proxyHost", $proxyPort)$renderSslPort$renderCredentials)"""

			protocol.getOrElse("")
		}

		def renderFollowRedirect = if (!configuration.http.followRedirect) s"$eol$indent.disableFollowRedirect" else ""

		def renderAutomaticReferer = if (!configuration.http.automaticReferer) s"$eol$indent.disableAutoReferer" else ""

		def renderHeaders = {
			def renderHeader(methodName: String, headerValue: String) = fast"""$eol$indent.$methodName("$headerValue")"""
			headers.toList.sorted.flatMap { case (headerName, headerValue) => baseHeaders.get(headerName).map(renderHeader(_, headerValue)) }.mkFastring
		}

		fast"""
		.baseURL("$baseUrl")$renderProxy$renderFollowRedirect$renderAutomaticReferer$renderHeaders""".toString
	}
}