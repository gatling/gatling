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
package io.gatling.http

import io.gatling.core.result.message.{ KO, Status }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.action.{ AddCookieBuilder, CookieDSL }
import io.gatling.http.cache.CacheHandling
import io.gatling.http.check.HttpCheckSupport
import io.gatling.http.config.HttpProtocolBuilder
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.request.BodyProcessors
import io.gatling.http.request.builder.{ Http, WebSocket }

object Predef extends HttpCheckSupport {
	type Request = com.ning.http.client.Request
	type Response = io.gatling.http.response.Response

	def http = HttpProtocolBuilder.default

	val Proxy = io.gatling.http.config.HttpProxyBuilder.apply _

	def http(requestName: Expression[String]) = new Http(requestName)
	def addCookie(cookie: CookieDSL) = new AddCookieBuilder(cookie.name, cookie.value, cookie.domain, cookie.path, cookie.expires.getOrElse(-1L), cookie.maxAge.getOrElse(-1))
	def flushSessionCookies = CookieHandling.flushSessionCookies
	def flushCookieJar = CookieHandling.flushCookieJar
	def flushHttpCache = CacheHandling.flushCache

	def websocket(requestName: Expression[String]) = new WebSocket(requestName)

	val HttpHeaderNames = HeaderNames
	val HttpHeaderValues = HeaderValues

	val gzipBody = BodyProcessors.gzip
	val streamBody = BodyProcessors.stream

	def dumpSessionOnFailure(status: Status, session: Session, request: Request, response: Response): List[String] = status match {
		case KO => List(session.toString)
		case _ => Nil
	}

	def Cookie = CookieDSL

	def ELFileBody = io.gatling.http.request.ELFileBody
	def StringBody = io.gatling.http.request.StringBody
	def RawFileBody = io.gatling.http.request.RawFileBody
	def ByteArrayBody = io.gatling.http.request.ByteArrayBody
	def InputStreamBody = io.gatling.http.request.InputStreamBody

	def StringBodyPart = io.gatling.http.request.BodyPart.stringBodyPart _
	def ByteArrayBodyPart = io.gatling.http.request.BodyPart.byteArrayBodyPart _
	def FileBodyPart = io.gatling.http.request.BodyPart.fileBodyPart _
	def RawFileBodyPart = io.gatling.http.request.BodyPart.rawFileBodyPart _
	def ELFileBodyPart = io.gatling.http.request.BodyPart.elFileBodyPart _
}
