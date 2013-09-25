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
package io.gatling.http

import io.gatling.core.result.message.{ KO, Status }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.action.{ AddCookieBuilder, HttpRequestActionBuilder }
import io.gatling.http.check.body.{ HttpBodyCssCheckBuilder, HttpBodyJsonPathCheckBuilder, HttpBodyRegexCheckBuilder, HttpBodyStringCheckBuilder, HttpBodyXPathCheckBuilder }
import io.gatling.http.check.checksum.HttpChecksumCheckBuilder
import io.gatling.http.check.header.{ HttpHeaderCheckBuilder, HttpHeaderRegexCheckBuilder }
import io.gatling.http.check.status.HttpStatusCheckBuilder
import io.gatling.http.check.time.HttpResponseTimeCheckBuilder
import io.gatling.http.check.url.CurrentLocationCheckBuilder
import io.gatling.http.config.{ HttpProtocol, HttpProtocolBuilder, HttpProxyBuilder }
import io.gatling.http.request.BodyProcessors
import io.gatling.http.request.builder.{ AbstractHttpRequestBuilder, HttpRequestBaseBuilder, WebSocketBaseBuilder }
import io.gatling.http.util.{ DefaultRequestLogger, DefaultWebSocketClient }

object Predef {
	type Request = com.ning.http.client.Request
	type Response = io.gatling.http.response.Response

	implicit def proxyBuilder2HttpProtocolBuilder(hpb: HttpProxyBuilder): HttpProtocolBuilder = hpb.toHttpProtocolBuilder
	implicit def proxyBuilder2HttpProtocol(hpb: HttpProxyBuilder): HttpProtocol = hpb.toHttpProtocolBuilder.build
	implicit def httpProtocolBuilder2HttpProtocol(builder: HttpProtocolBuilder): HttpProtocol = builder.build
	implicit def requestBuilder2ActionBuilder(requestBuilder: AbstractHttpRequestBuilder[_]) = HttpRequestActionBuilder(requestBuilder)

	def http(requestName: Expression[String]) = HttpRequestBaseBuilder.http(requestName)
	def addCookie(name: Expression[String], value: Expression[String], domain: Option[Expression[String]] = None, path: Option[Expression[String]]) = new AddCookieBuilder(name, value, domain, path)
	def http = HttpProtocolBuilder.default
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
	def responseTimeInMillis = HttpResponseTimeCheckBuilder.responseTimeInMillis
	def latencyInMillis = HttpResponseTimeCheckBuilder.latencyInMillis

	val HttpHeaderNames = Headers.Names
	val HttpHeaderValues = Headers.Values

	val gzipBody = BodyProcessors.gzip
	val streamBody = BodyProcessors.stream

	val requestUrl = (request: Request) => List(request.getUrl)
	val requestRawUrl = (request: Request) => List(request.getRawUrl)
	val responseStatusCode = (response: Response) => List(response.getStatusCode.toString)
	val responseStatusText = (response: Response) => List(response.getStatusText)
	val responseContentType = (response: Response) => List(response.getContentType)
	val responseContentLength = (response: Response) => List(response.getHeader(Headers.Names.CONTENT_LENGTH))
	val responseUri = (response: Response) => List(response.getUri.toString)

	def dumpSessionOnFailure(status: Status, session: Session, request: Request, response: Response): List[String] = status match {
		case KO => List(session.toString)
		case _ => Nil
	}

	def ELFileBody = io.gatling.http.request.ELFileBody
	def StringBody = io.gatling.http.request.StringBody
	def RawFileBody = io.gatling.http.request.RawFileBody
	def ByteArrayBody = io.gatling.http.request.ByteArrayBody
	def InputStreamBody = io.gatling.http.request.InputStreamBody

	def StringBodyPart = io.gatling.http.request.StringBodyPart
	def ByteArrayBodyPart = io.gatling.http.request.ByteArrayBodyPart
	def FileBodyPart = io.gatling.http.request.FileBodyPart
	def RawFileBodyPart = io.gatling.http.request.RawFileBodyPart
	def ELFileBodyPart = io.gatling.http.request.ELFileBodyPart

	def websocket(actionName: Expression[String]) = WebSocketBaseBuilder.websocket(actionName)
	implicit val defaultWebSocketClient = DefaultWebSocketClient
	implicit val defaultRequestLogger = DefaultRequestLogger
}
