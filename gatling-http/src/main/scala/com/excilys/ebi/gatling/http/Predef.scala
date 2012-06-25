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
package com.excilys.ebi.gatling.http

import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.http.check.body.{ HttpBodyXPathCheckBuilder, HttpBodyRegexCheckBuilder, HttpBodyJsonPathCheckBuilder, HttpBodyCssCheckBuilder }
import com.excilys.ebi.gatling.http.check.bodypart.HttpBodyPartCheckBuilder
import com.excilys.ebi.gatling.http.check.header.{ HttpHeaderRegexCheckBuilder, HttpHeaderCheckBuilder }
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder
import com.excilys.ebi.gatling.http.config.{ HttpProxyBuilder, HttpProtocolConfigurationBuilder, HttpProtocolConfiguration }
import com.excilys.ebi.gatling.http.request.builder.HttpRequestBaseBuilder
import com.ning.http.client.{ Response, Request }

object Predef {

	def http(requestName: String) = HttpRequestBaseBuilder.http(requestName)

	def httpConfig = HttpProtocolConfigurationBuilder.httpConfig
	implicit def toHttpProtocolConfigurationBuilder(hpb: HttpProxyBuilder): HttpProtocolConfigurationBuilder = HttpProxyBuilder.toHttpProtocolConfigurationBuilder(hpb)
	implicit def toHttpProtocolConfiguration(hpb: HttpProxyBuilder): HttpProtocolConfiguration = HttpProxyBuilder.toHttpProtocolConfigurationBuilder(hpb)
	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder): HttpProtocolConfiguration = HttpProtocolConfigurationBuilder.toHttpProtocolConfiguration(builder)

	def regex(pattern: EvaluatableString) = HttpBodyRegexCheckBuilder.regex(pattern)
	def xpath(expression: EvaluatableString, namespaces: List[(String, String)] = Nil) = HttpBodyXPathCheckBuilder.xpath(expression, namespaces)
	def css(selector: EvaluatableString) = HttpBodyCssCheckBuilder.css(selector)
	def jsonPath(expression: EvaluatableString) = HttpBodyJsonPathCheckBuilder.jsonPath(expression)
	def header(headerName: EvaluatableString) = HttpHeaderCheckBuilder.header(headerName)
	def headerRegex(headerName: EvaluatableString, pattern: EvaluatableString) = HttpHeaderRegexCheckBuilder.headerRegex(headerName, pattern)
	def status = HttpStatusCheckBuilder.status
	def md5 = HttpBodyPartCheckBuilder.checksum("MD5")
	def sha1 = HttpBodyPartCheckBuilder.checksum("SHA-1")

	val requestUrl = (request: Request) => List(request.getUrl())
	val requestRawUrl = (request: Request) => List(request.getRawUrl())
	val responseStatusCode = (response: Response) => List(response.getStatusCode.toString)
}