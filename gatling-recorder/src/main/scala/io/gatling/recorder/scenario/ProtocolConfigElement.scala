/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.recorder.scenario

import io.gatling.core.util.StringHelper.eol
import io.gatling.http.Headers
import io.gatling.recorder.config.RecorderConfiguration.configuration

import grizzled.slf4j.Logging

object ProtocolConfigElement {
	val baseHeaders = Map(Headers.Names.ACCEPT -> "acceptHeader",
		Headers.Names.ACCEPT_CHARSET -> "acceptCharsetHeader",
		Headers.Names.ACCEPT_ENCODING -> "acceptEncodingHeader",
		Headers.Names.ACCEPT_LANGUAGE -> "acceptLanguageHeader",
		Headers.Names.AUTHORIZATION -> "authorizationHeader",
		Headers.Names.CONNECTION -> "connection",
		Headers.Names.DO_NOT_TRACK -> "doNotTrackHeader",
		Headers.Names.USER_AGENT -> "userAgentHeader")
}

class ProtocolConfigElement(baseUrl: String, followRedirect: Boolean, automaticReferer: Boolean, baseHeaders: Map[String, String]) extends ScenarioElement with Logging {

	override def toString = {
		val indent = "\t\t\t"

		val sb = new StringBuilder

		def appendLine(line: String) {
			sb.append(indent).append(line).append(eol)
		}

		appendLine(".baseURL(\"" + baseUrl + "\")")

		for {
			proxyHost <- configuration.proxy.outgoing.host
			proxyPort <- configuration.proxy.outgoing.port
		} {
			val sslPort = configuration.proxy.outgoing.sslPort.map(proxySslPort => ".httpsPort(" + proxySslPort + ")").getOrElse("")
			appendLine(".proxy(\"" + proxyHost + "\", " + proxyPort + ")" + sslPort)
		}

		for {
			proxyUsername <- configuration.proxy.outgoing.username
			proxyPassword <- configuration.proxy.outgoing.password
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

		baseHeaders.toList.sorted.foreach { case (headerName, headerValue) => ProtocolConfigElement.baseHeaders.get(headerName).foreach(appendHeader(_, headerValue)) }
		sb.toString
	}
}