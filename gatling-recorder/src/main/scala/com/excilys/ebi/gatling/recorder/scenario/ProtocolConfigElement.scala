/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.scenario

import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.recorder.config.ProxyConfig

import grizzled.slf4j.Logging

class ProtocolConfigElement(baseUrl: String, proxy: ProxyConfig, followRedirect: Boolean, automaticReferer: Boolean, baseHeaders: Map[String, String]) extends ScenarioElement with Logging {

	override def toString = {
		val sb = new StringBuilder

		sb.append(""".baseURL("""").append(baseUrl).append("""")""").append(END_OF_LINE)

		for {
			proxyHost <- proxy.host
			proxyPort <- proxy.port
		} {
			sb.append(""".proxy("""").append(proxyHost).append("""", """).append(proxyPort).append(")")
			proxy.sslPort.map(proxySslPort => sb.append(".httpsPort(").append(proxySslPort).append(")"))
			sb.append(END_OF_LINE)
		}

		for {
			proxyUsername <- proxy.username
			proxyPassword <- proxy.password
		} {
			sb.append(""".credentials("""").append(proxyUsername).append("""", """").append(proxyPassword).append("""")""").append(END_OF_LINE)
		}

		if (!followRedirect)
			sb.append(".disableFollowRedirect").append(END_OF_LINE)

		if (!automaticReferer)
			sb.append(".disableAutomaticReferer").append(END_OF_LINE)

		val indent = "\t\t\t"
		baseHeaders.foreach {
			case (headerName, headerValue) => headerName match {
				case Headers.Names.ACCEPT => sb.append(indent).append(""".acceptHeader("""").append(headerValue).append("""")""").append(END_OF_LINE)
				case Headers.Names.ACCEPT_CHARSET => sb.append(indent).append(""".acceptCharsetHeader("""").append(headerValue).append("""")""").append(END_OF_LINE)
				case Headers.Names.ACCEPT_ENCODING => sb.append(indent).append(""".acceptEncodingHeader("""").append(headerValue).append("""")""").append(END_OF_LINE)
				case Headers.Names.ACCEPT_LANGUAGE => sb.append(indent).append(""".acceptLanguageHeader("""").append(headerValue).append("""")""").append(END_OF_LINE)
				case Headers.Names.HOST => sb.append(indent).append(""".hostHeader("""").append(headerValue).append("""")""").append(END_OF_LINE)
				case Headers.Names.USER_AGENT => sb.append(indent).append(""".userAgentHeader("""").append(headerValue).append("""")""").append(END_OF_LINE)
				case name => warn("Base header not supported " + name)
			}
		}
		sb.toString
	}
}