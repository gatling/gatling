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

import com.excilys.ebi.gatling.core.util.StringHelper.{ EMPTY, END_OF_LINE }
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.recorder.config.ProxyConfig

import grizzled.slf4j.Logging

class ProtocolConfigElement(baseUrl: String, proxy: ProxyConfig, followRedirect: Boolean, automaticReferer: Boolean, baseHeaders: Map[String, String]) extends ScenarioElement with Logging {

	override def toString = {
		val indent = "\t\t\t"

		val sb = new StringBuilder

		def appendLine(line: String) {
			sb.append(indent).append(line).append(END_OF_LINE)
		}

		appendLine(".baseURL(\"" + baseUrl + "\")")

		for {
			proxyHost <- proxy.host
			proxyPort <- proxy.port
		} {
			val sslPort = proxy.sslPort.map(proxySslPort => ".httpsPort(" + proxySslPort + ")").getOrElse(EMPTY)
			appendLine(".proxy(\"" + proxyHost + "\", " + proxyPort + ")" + sslPort)
		}

		for {
			proxyUsername <- proxy.username
			proxyPassword <- proxy.password
		} {
			appendLine(".credentials(\"" + proxyUsername + "\", " + proxyPassword + "\")")
		}

		if (!followRedirect)
			appendLine(".disableFollowRedirect")

		if (!automaticReferer)
			appendLine(".disableAutomaticReferer")

		def appendHeader(methodName: String, headerValue: String) {
			appendLine("." + methodName + "(\"" + headerValue + "\")")
		}

		baseHeaders.foreach {
			case (headerName, headerValue) => headerName match {
				case Headers.Names.ACCEPT => appendHeader("acceptHeader", headerValue)
				case Headers.Names.ACCEPT_CHARSET => appendHeader("acceptCharsetHeader", headerValue)
				case Headers.Names.ACCEPT_ENCODING => appendHeader("acceptEncodingHeader", headerValue)
				case Headers.Names.ACCEPT_LANGUAGE => appendHeader("acceptLanguageHeader", headerValue)
				case Headers.Names.AUTHORIZATION => appendHeader("authorizationHeader", headerValue)
				case Headers.Names.DO_NOT_TRACK => appendHeader("doNotTrackHeader", headerValue)
				case Headers.Names.USER_AGENT => appendHeader("userAgentHeader", headerValue)
				case name => warn("Base header not supported " + name)
			}
		}
		sb.toString
	}
}