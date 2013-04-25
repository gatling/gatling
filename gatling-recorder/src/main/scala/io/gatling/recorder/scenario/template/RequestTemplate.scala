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

import com.dongxiguo.fastring.Fastring.Implicits._

object RequestTemplate {

	val tripleQuotes = '"'.toString * 3

	def headersBlockName(id: Int) = fast"headers_$id"
	
	def render(simulationClass: String,
		id: Int,
		method: String,
		printedUrl: String,
		headersId: Option[Int],
		credentials: Option[(String, String)],
		queryParams: List[(String, String)],
		requestBodyOrParams: Option[Either[String, List[(String, String)]]],
		statusCode: Int) = {

		def renderHeaders = headersId
			.map { id =>
				s"""
			.headers(${headersBlockName(id)})"""
			}.getOrElse("")

		def renderQueryParams = queryParams.map {
			case (name, value) => fast"""
			.queryParam($tripleQuotes$name$tripleQuotes, $tripleQuotes$value$tripleQuotes)"""
		}.mkFastring

		def renderBodyOrParams = requestBodyOrParams.map {
			_ match {
				case Left(_) => fast"""
			.rawFileBody("${simulationClass}_request_$id.txt")"""
				case Right(params) => params.map {
					case (key, value) => fast"""
			.param($tripleQuotes$key$tripleQuotes, $tripleQuotes$value$tripleQuotes)"""
				}.mkFastring
			}
		}.getOrElse(fast"")

		def renderCredentials = credentials.map {
			case (username, password) => s"""
			.basicAuth($tripleQuotes$username$tripleQuotes,$tripleQuotes$password$tripleQuotes)"""
		}.getOrElse("")

		def renderStatusCheck =
			if (statusCode > 210 || statusCode < 200) {
				s"""
			.check(status.is($statusCode))"""
			} else ""

		fast"""exec(http("request_$id")
			.${method.toLowerCase}("$printedUrl")$renderHeaders$renderQueryParams$renderBodyOrParams$renderCredentials$renderStatusCheck)""".toString
	}
}