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
package com.excilys.ebi.gatling.http

import com.excilys.ebi.gatling.core.session.Expression
import com.excilys.ebi.gatling.http.action.{ AddCookiesBuilder, HttpRequestActionBuilder }
import com.excilys.ebi.gatling.http.check.after.HttpBodyResponseTimeCheckBuilder
import com.excilys.ebi.gatling.http.check.body.{ HttpBodyCssCheckBuilder, HttpBodyJsonPathCheckBuilder, HttpBodyRegexCheckBuilder, HttpBodyStringCheckBuilder, HttpBodyXPathCheckBuilder }
import com.excilys.ebi.gatling.http.check.bodypart.HttpChecksumCheckBuilder
import com.excilys.ebi.gatling.http.check.header.{ HttpHeaderCheckBuilder, HttpHeaderRegexCheckBuilder }
import com.excilys.ebi.gatling.http.check.status.{ CurrentLocationCheckBuilder, HttpStatusCheckBuilder }
import com.excilys.ebi.gatling.http.config.{ HttpProtocolConfiguration, HttpProtocolConfigurationBuilder, HttpProxyBuilder }
import com.excilys.ebi.gatling.http.request.builder.{ AbstractHttpRequestBuilder, HttpRequestBaseBuilder }
import com.excilys.ebi.gatling.http.response.ExtendedResponse

object Predef {
	type Request = com.ning.http.client.Request
	type Response = com.ning.http.client.Response
	type ExtendedResponse = com.excilys.ebi.gatling.http.response.ExtendedResponse
	type Cookie = com.excilys.ebi.gatling.http.action.Cookie

	implicit def proxyBuilder2HttpProtocolConfigurationBuilder(hpb: HttpProxyBuilder): HttpProtocolConfigurationBuilder = hpb.toHttpProtocolConfigurationBuilder
	implicit def proxyBuilder2HttpProtocolConfiguration(hpb: HttpProxyBuilder): HttpProtocolConfiguration = hpb.toHttpProtocolConfigurationBuilder.build
	implicit def httpProtocolConfigurationBuilder2HttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder): HttpProtocolConfiguration = builder.build
	implicit def requestBuilder2ActionBuilder(requestBuilder: AbstractHttpRequestBuilder[_]): HttpRequestActionBuilder = requestBuilder.toActionBuilder

	def http(requestName: Expression[String]) = HttpRequestBaseBuilder.http(requestName)
	def addCookies(url: Expression[String], cookie: Cookie, cookies: Cookie*) = AddCookiesBuilder(url, cookie :: cookies.toList)
	def httpConfig = HttpProtocolConfigurationBuilder.httpConfig
	def regex(pattern: Expression[String]) = HttpBodyRegexCheckBuilder.regex(pattern)
	def xpath(expression: Expression[String], namespaces: List[(String, String)] = Nil) = HttpBodyXPathCheckBuilder.xpath(expression, namespaces)
	def css(selector: Expression[String]) = HttpBodyCssCheckBuilder.css(selector, None)
	def css(selector: Expression[String], nodeAttribute: String) = HttpBodyCssCheckBuilder.css(selector, Some(nodeAttribute))
	def jsonPath(expression: Expression[String]) = HttpBodyJsonPathCheckBuilder.jsonPath(expression)
	def bodyString = HttpBodyStringCheckBuilder.bodyString
	def header(headerName: Expression[String]) = HttpHeaderCheckBuilder.header(headerName)
	def headerRegex(headerName: Expression[String], pattern: Expression[String]) = HttpHeaderRegexCheckBuilder.headerRegex(headerName, pattern)
	def status = HttpStatusCheckBuilder.status
	def currentLocation = CurrentLocationCheckBuilder.currentLocation
	def md5 = HttpChecksumCheckBuilder.md5
	def sha1 = HttpChecksumCheckBuilder.sha1
	def responseTimeInMillis = HttpBodyResponseTimeCheckBuilder.responseTimeInMillis
	def latencyInMillis = HttpBodyResponseTimeCheckBuilder.latencyInMillis

	val requestUrl = (request: Request) => List(request.getUrl)
	val requestRawUrl = (request: Request) => List(request.getRawUrl)
	val responseStatusCode = (response: ExtendedResponse) => List(response.getStatusCode.toString)
	val responseStatusText = (response: ExtendedResponse) => List(response.getStatusText)
	val responseContentType = (response: ExtendedResponse) => List(response.getContentType)
	val responseContentLength = (response: ExtendedResponse) => List(response.getHeader(Headers.Names.CONTENT_LENGTH))
	val responseUri = (response: ExtendedResponse) => List(response.getUri.toString)
}