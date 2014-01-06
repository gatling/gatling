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
import io.gatling.http.action.AddCookieBuilder
import io.gatling.http.check.HttpCheckSupport
import io.gatling.http.config.HttpProtocolBuilder
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.request.BodyProcessors
import io.gatling.http.request.builder.{ HttpRequestBaseBuilder, WebSocketBaseBuilder }
import io.gatling.http.util.{ DefaultRequestLogger, DefaultWebSocketClient }

object Predef extends HttpCheckSupport {
	type Request = com.ning.http.client.Request
	type Response = io.gatling.http.response.Response

	def http = HttpProtocolBuilder.default

	val Proxy = io.gatling.http.config.HttpProxyBuilder.apply _

	def http(requestName: Expression[String]) = new HttpRequestBaseBuilder(requestName)
	def addCookie(name: Expression[String], value: Expression[String], domain: Option[Expression[String]] = None, path: Option[Expression[String]] = None, maxAge: Int = -1) = new AddCookieBuilder(name, value, domain, path, maxAge)
	def flushSessionCookies = CookieHandling.flushSessionCookies

	def websocket(actionName: Expression[String]) = WebSocketBaseBuilder.websocket(actionName)
	implicit val defaultWebSocketClient = DefaultWebSocketClient
	implicit val defaultRequestLogger = DefaultRequestLogger

	val HttpHeaderNames = HeaderNames
	val HttpHeaderValues = HeaderValues

	val gzipBody = BodyProcessors.gzip
	val streamBody = BodyProcessors.stream

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
}
