/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder

import java.net.URI

import com.ning.http.client.{ Request, RequestBuilder => AHCRequestBuilder }
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.HeaderNames
import io.gatling.http.ahc.ConnectionPoolKeyStrategy
import io.gatling.http.config.HttpProtocol
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.referer.RefererHandling

abstract class RequestExpressionBuilder(commonAttributes: CommonAttributes, protocol: HttpProtocol) extends StrictLogging {

	def makeAbsolute(url: String): Validation[String]

	def buildURI(session: Session): Validation[URI] = {

		def createURI(url: String): Validation[URI] =
			try
				URI.create(url).success
			catch {
				case e: Exception => s"url $url can't be parsed into a URI: ${e.getMessage}".failure
			}

		commonAttributes.urlOrURI match {
			case Left(url) => url(session).flatMap(makeAbsolute).flatMap(createURI)
			case Right(uri) => uri.success
		}
	}

	def configureProxy(uri: URI)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {
		if (!protocol.proxyPart.proxyExceptions.contains(uri.getHost)) {
			if (uri.getScheme == "http" || uri.getScheme == "ws")
				commonAttributes.proxy.orElse(protocol.proxyPart.proxy).foreach(requestBuilder.setProxyServer)
			else
				commonAttributes.secureProxy.orElse(protocol.proxyPart.secureProxy).foreach(requestBuilder.setProxyServer)
		}
		requestBuilder.success
	}

	def configureCookies(session: Session, uri: URI)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {
		CookieHandling.getStoredCookies(session, uri).foreach(requestBuilder.addCookie)
		requestBuilder.success
	}

	def configureQuery(session: Session, uri: URI)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {

		if (!commonAttributes.queryParams.isEmpty)
			commonAttributes.queryParams.resolveFluentStringsMap(session).map(requestBuilder.setQueryParameters(_).setURI(uri))
		else
			requestBuilder.setURI(uri).success
	}

	def configureVirtualHost(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] =
		commonAttributes.virtualHost.orElse(protocol.enginePart.virtualHost) match {
			case Some(virtualHost) => virtualHost(session).map(requestBuilder.setVirtualHost)
			case _ => requestBuilder.success
		}

	def configureHeaders(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {

		val headers = protocol.requestPart.baseHeaders ++ commonAttributes.headers

		val requestBuilderWithHeaders = headers.foldLeft(requestBuilder.success) { (requestBuilder, header) =>
			val (key, value) = header
			for {
				requestBuilder <- requestBuilder
				value <- value(session)
			} yield requestBuilder.addHeader(key, value)
		}

		val additionalRefererHeader =
			if (headers.contains(HeaderNames.REFERER))
				None
			else
				RefererHandling.getStoredReferer(session)

		additionalRefererHeader match {
			case Some(referer) => requestBuilderWithHeaders.map(_.addHeader(HeaderNames.REFERER, referer))
			case _ => requestBuilderWithHeaders
		}
	}

	def configureRealm(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] =
		commonAttributes.realm.orElse(protocol.requestPart.basicAuth) match {
			case Some(realm) => realm(session).map(requestBuilder.setRealm)
			case None => requestBuilder.success
		}

	protected def configureRequestBuilder(session: Session, uri: URI, requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] =
		configureProxy(uri)(requestBuilder)
			.flatMap(configureCookies(session, uri))
			.flatMap(configureQuery(session, uri))
			.flatMap(configureVirtualHost(session))
			.flatMap(configureHeaders(session))
			.flatMap(configureRealm(session))

	def build: Expression[Request] =
		(session: Session) => {
			val useRawUrl = commonAttributes.useRawUrl.getOrElse(configuration.http.ahc.useRawUrl)
			val requestBuilder = new AHCRequestBuilder(commonAttributes.method, useRawUrl)

			requestBuilder.setBodyEncoding(configuration.core.encoding)

			if (!protocol.enginePart.shareConnections)
				requestBuilder.setConnectionPoolKeyStrategy(new ConnectionPoolKeyStrategy(session))

			protocol.enginePart.localAddress.foreach(requestBuilder.setLocalInetAddress)

			commonAttributes.address.foreach(requestBuilder.setInetAddress)

			try
				for {
					uri <- buildURI(session)
					rb <- configureRequestBuilder(session, uri, requestBuilder)
				} yield rb.build

			catch {
				case e: Exception =>
					logger.warn("Failed to build request", e)
					s"Failed to build request: ${e.getMessage}".failure
			}
		}
}
