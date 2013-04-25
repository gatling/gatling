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
package io.gatling.recorder.scenario

import io.gatling.http.Headers
import io.gatling.recorder.scenario.template.ProtocolConfigTemplate

object ProtocolConfigElement {

	val baseHeaders = Map(
		Headers.Names.ACCEPT -> "acceptHeader",
		Headers.Names.ACCEPT_CHARSET -> "acceptCharsetHeader",
		Headers.Names.ACCEPT_ENCODING -> "acceptEncodingHeader",
		Headers.Names.ACCEPT_LANGUAGE -> "acceptLanguageHeader",
		Headers.Names.AUTHORIZATION -> "authorizationHeader",
		Headers.Names.CONNECTION -> "connection",
		Headers.Names.DO_NOT_TRACK -> "doNotTrackHeader",
		Headers.Names.USER_AGENT -> "userAgentHeader")
}
class ProtocolConfigElement(baseUrl: String, followRedirect: Boolean, automaticReferer: Boolean, headers: Map[String, String]) extends ScenarioElement {

	override def toString = ProtocolConfigTemplate.render(baseUrl, followRedirect, automaticReferer, headers)

}