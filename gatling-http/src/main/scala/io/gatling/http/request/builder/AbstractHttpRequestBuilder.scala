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
package io.gatling.http.request.builder

import com.ning.http.client.{ Realm, Request, RequestBuilder }
import com.ning.http.client.ProxyServer.Protocol

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ EL, Expression, Session }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation, ValidationList }
import io.gatling.http.Headers.{ Names => HeaderNames, Values => HeaderValues }
import io.gatling.http.action.HttpRequestActionBuilder
import io.gatling.http.ahc.GatlingConnectionPoolKeyStrategy
import io.gatling.http.check.HttpCheck
import io.gatling.http.config.HttpProtocolConfiguration
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.referer.RefererHandling
import io.gatling.http.util.HttpHelper

case class HttpAttributes(
	requestName: Expression[String],
	method: String,
	url: Expression[String],
	queryParams: List[HttpParam] = Nil,
	headers: Map[String, Expression[String]] = Map.empty,
	realm: Option[Expression[Realm]] = None,
	checks: List[HttpCheck] = Nil)

object AbstractHttpRequestBuilder {

	val jsonHeaderValueExpression = EL.compile[String](HeaderValues.APPLICATION_JSON)
	val xmlHeaderValueExpression = EL.compile[String](HeaderValues.APPLICATION_XML)
}

/**
 * This class serves as model for all HttpRequestBuilders
 *
 * @param httpAttributes the base HTTP attributes
 */
abstract class AbstractHttpRequestBuilder[B <: AbstractHttpRequestBuilder[B]](httpAttributes: HttpAttributes) {

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param requestName is the name of the request
	 * @param url the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param credentials sets the credentials in case of Basic HTTP Authentication
	 */
	private[http] def newInstance(httpAttributes: HttpAttributes): B

	/**
	 * Stops defining the request and adds checks on the response
	 *
	 * @param checks the checks that will be performed on the response
	 */
	def check(checks: HttpCheck*): B = newInstance(httpAttributes.copy(checks = httpAttributes.checks ::: checks.toList))

	/**
	 * Adds a query parameter to the request
	 *
	 * @param param is a query parameter
	 */
	def queryParam(key: Expression[String], value: Expression[String]): B = {
		val httpParam: HttpParam = (key, (session: Session) => value(session).map(Seq(_)))
		queryParam(httpParam)
	}

	def multiValuedQueryParam(key: Expression[String], values: Expression[Seq[String]]): B = {
		val httpParam: HttpParam = (key, values)
		queryParam(httpParam)
	}

	private def queryParam(param: HttpParam): B = newInstance(httpAttributes.copy(queryParams = param :: httpAttributes.queryParams))

	/**
	 * Adds a header to the request
	 *
	 * @param header the header to add, eg: ("Content-Type", "application/json")
	 */
	def header(name: String, value: Expression[String]): B = newInstance(httpAttributes.copy(headers = httpAttributes.headers + (name -> value)))

	/**
	 * Adds several headers to the request at the same time
	 *
	 * @param newHeaders a scala map containing the headers to add
	 */
	def headers(newHeaders: Map[String, String]): B = newInstance(httpAttributes.copy(headers = httpAttributes.headers ++ newHeaders.mapValues(EL.compile[String])))

	/**
	 * Adds Accept and Content-Type headers to the request set with "application/json" values
	 */
	def asJSON: B = header(HeaderNames.ACCEPT, AbstractHttpRequestBuilder.jsonHeaderValueExpression).header(HeaderNames.CONTENT_TYPE, AbstractHttpRequestBuilder.jsonHeaderValueExpression)

	/**
	 * Adds Accept and Content-Type headers to the request set with "application/xml" values
	 */
	def asXML: B = header(HeaderNames.ACCEPT, AbstractHttpRequestBuilder.xmlHeaderValueExpression).header(HeaderNames.CONTENT_TYPE, AbstractHttpRequestBuilder.xmlHeaderValueExpression)

	/**
	 * Adds BASIC authentication to the request
	 *
	 * @param username the username needed
	 * @param password the password needed
	 */
	def basicAuth(username: Expression[String], password: Expression[String]): B = newInstance(httpAttributes.copy(realm = Some(HttpHelper.buildRealm(username, password))))

	/**
	 * This method actually fills the request builder to avoid race conditions
	 *
	 * @param session the session of the current scenario
	 */
	protected def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): Validation[RequestBuilder] = {

		def makeAbsolute(url: String): Validation[String] =
			if (url.startsWith(Protocol.HTTP.getProtocol))
				url.success
			else
				protocolConfiguration.baseURL.map(baseURL => (baseURL + url).success).getOrElse(s"No protocolConfiguration.baseURL defined but provided url is relative : $url".failure)

		def configureUrlCookiesAndProxy(url: String)(implicit requestBuilder: RequestBuilder): Validation[RequestBuilder] = {

			val proxy = if (url.startsWith(Protocol.HTTPS.getProtocol))
				protocolConfiguration.securedProxy
			else
				protocolConfiguration.proxy

			proxy.map(requestBuilder.setProxyServer)

			CookieHandling.getStoredCookies(session, url).foreach(requestBuilder.addCookie)

			requestBuilder.setUrl(url).success
		}

		def configureQueryParams(requestBuilder: RequestBuilder): Validation[RequestBuilder] =
			HttpHelper.httpParamsToFluentMap(httpAttributes.queryParams, session).map(requestBuilder.setQueryParameters)

		def configureHeaders(requestBuilder: RequestBuilder): Validation[RequestBuilder] = {

			val resolvedHeaders = httpAttributes.headers.map {
				case (key, value) =>
					for {
						resolvedValue <- value(session)
					} yield key -> resolvedValue
			}
				.toList
				.sequence

			resolvedHeaders.map { headers =>
				val newHeaders = RefererHandling.addStoredRefererHeader(protocolConfiguration.baseHeaders ++ headers, session, protocolConfiguration)
				newHeaders.foreach { case (headerName, headerValue) => requestBuilder.addHeader(headerName, headerValue) }
				requestBuilder
			}
		}

		def configureRealm(requestBuilder: RequestBuilder): Validation[RequestBuilder] = {

			val realm = httpAttributes.realm.orElse(protocolConfiguration.basicAuth)

			realm match {
				case Some(realm) => realm(session).map(requestBuilder.setRealm)
				case None => requestBuilder.success
			}
		}

		implicit val requestBuilder = new RequestBuilder(httpAttributes.method, configuration.http.useRawUrl).setBodyEncoding(configuration.simulation.encoding)

		if (!protocolConfiguration.shareConnections) requestBuilder.setConnectionPoolKeyStrategy(new GatlingConnectionPoolKeyStrategy(session))

		httpAttributes.url(session)
			.flatMap(makeAbsolute)
			.flatMap(configureUrlCookiesAndProxy)
			.flatMap(configureQueryParams)
			.flatMap(configureHeaders)
			.flatMap(configureRealm)
	}

	/**
	 * This method builds the request that will be sent
	 *
	 * @param session the session of the current scenario
	 */
	private[http] def build(session: Session, protocolConfiguration: HttpProtocolConfiguration): Validation[Request] = getAHCRequestBuilder(session, protocolConfiguration).map(_.build)

	private[gatling] def toActionBuilder = HttpRequestActionBuilder(httpAttributes.requestName, this, httpAttributes.checks)
}
