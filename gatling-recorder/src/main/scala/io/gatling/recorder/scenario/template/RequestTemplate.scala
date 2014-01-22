/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.core.util.StringHelper.emptyFastring
import io.gatling.recorder.scenario.{ RequestBody, RequestBodyBytes, RequestBodyParams }
import io.gatling.recorder.scenario.RequestElement

object RequestTemplate {

	val builtInHttpMethods = List("GET", "PUT", "PATCH", "HEAD", "DELETE", "OPTIONS", "POST")

	def headersBlockName(id: Int) = fast"headers_$id"

	def render(simulationClass: String, request: RequestElement) = {

		def renderMethod =
			if (builtInHttpMethods.contains(request.method)) {
				fast"${request.method.toLowerCase}($renderUrl)"
			} else {
				fast"""httpRequestWithBody("$request.method", Left($renderUrl))"""
			}

		def renderUrl = fast"""$tripleQuotes${request.printedUrl}$tripleQuotes"""

		def renderHeaders = request.filteredHeadersId
			.map { id =>
				s"""
			.headers(${headersBlockName(id)})"""
			}.getOrElse("")

		def renderBodyOrParams = request.body.map {
			case RequestBodyBytes(_) => fast"""
			.body(RawFileBody("${simulationClass}_request_${request.id}.txt"))"""
			case RequestBodyParams(params) => params.map {
				case (key, value) => fast"""
			.param($tripleQuotes$key$tripleQuotes, $tripleQuotes$value$tripleQuotes)"""
			}.mkFastring
		}.getOrElse(emptyFastring)

		def renderCredentials = request.basicAuthCredentials.map {
			case (username, password) => s"""
			.basicAuth($tripleQuotes$username$tripleQuotes,$tripleQuotes$password$tripleQuotes)"""
		}.getOrElse("")

		def renderStatusCheck =
			if (request.statusCode > 210 || request.statusCode < 200) {
				fast"""
			.check(status.is(${request.statusCode}))"""
			} else ""

		fast"""exec(http("request_${request.id}")
			.$renderMethod$renderHeaders$renderBodyOrParams$renderCredentials$renderStatusCheck)""".toString
	}
}
