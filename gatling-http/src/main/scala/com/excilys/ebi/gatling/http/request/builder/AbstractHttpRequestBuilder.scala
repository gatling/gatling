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
package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.session.{ Expression, Session }
import com.excilys.ebi.gatling.core.util.FlattenableValidations
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames, Values => HeaderValues }
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.http.referer.RefererHandling
import com.excilys.ebi.gatling.http.util.HttpHelper
import com.ning.http.client.{ Request, RequestBuilder }
import com.ning.http.client.ProxyServer.Protocol
import com.ning.http.client.Realm
import com.ning.http.client.Realm.AuthScheme

import scalaz.Scalaz.ToValidationV
import scalaz.Validation

case class HttpAttributes(
	requestName: Expression[String],
	method: String,
	url: Expression[String],
	queryParams: List[HttpParam] = Nil,
	headers: Map[String, Expression[String]] = Map.empty,
	realm: Option[Expression[Realm]] = None,
	checks: List[HttpCheck[_]] = Nil)

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
	def check(checks: HttpCheck[_]*): B = newInstance(httpAttributes.copy(checks = httpAttributes.checks ::: checks.toList))

	/**
	 * Adds a query parameter to the request
	 *
	 * The value is a session attribute with the same key
	 *
	 * @param key the key of the parameter
	 */
	def queryParam(key: String): B = queryParam(Expression.compile[String](key), (s: Session) => s.safeGetAs[String](key))

	/**
	 * Adds a query parameter to the request
	 *
	 * @param param is a query parameter
	 */
	def queryParam(key: Expression[String], value: Expression[String]): B = {
		val httpParam: HttpParam = (key, (session: Session) => value(session).map(Seq(_)))
		queryParam(httpParam)
	}

	def multiValuedQueryParam(key: String): B = multiValuedQueryParam(Expression.compile[String](key), key)

	def multiValuedQueryParam(key: Expression[String], value: String): B = {
		val httpParam: HttpParam = (key, Expression.compile[Seq[String]](value))
		queryParam(httpParam)
	}

	def multiValuedQueryParam(key: Expression[String], values: Seq[String]): B = {
		val httpParam: HttpParam = (key, (s: Session) => values.success)
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
	def header(header: (String, String)): B = newInstance(httpAttributes.copy(headers = httpAttributes.headers + (header._1 -> Expression.compile[String](header._2))))

	/**
	 * Adds several headers to the request at the same time
	 *
	 * @param newHeaders a scala map containing the headers to add
	 */
	def headers(newHeaders: Map[String, String]): B = newInstance(httpAttributes.copy(headers = httpAttributes.headers ++ newHeaders.mapValues(Expression.compile[String](_))))

	/**
	 * Adds Accept and Content-Type headers to the request set with "application/json" values
	 */
	def asJSON: B = header(HeaderNames.ACCEPT, HeaderValues.APPLICATION_JSON).header(HeaderNames.CONTENT_TYPE, HeaderValues.APPLICATION_JSON)

	/**
	 * Adds Accept and Content-Type headers to the request set with "application/xml" values
	 */
	def asXML: B = header(HeaderNames.ACCEPT, HeaderValues.APPLICATION_XML).header(HeaderNames.CONTENT_TYPE, HeaderValues.APPLICATION_XML)

	/**
	 * Adds BASIC authentication to the request
	 *
	 * @param username the username needed
	 * @param password the password needed
	 */
	def basicAuth(username: Expression[String], password: Expression[String]): B = {

		def buildRealm(session: Session) =
			for {
				usernameValue <- username(session)
				passwordValue <- password(session)
			} yield new Realm.RealmBuilder().setPrincipal(usernameValue).setPassword(passwordValue).setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC).build

		newInstance(httpAttributes.copy(realm = Some(buildRealm _)))
	}

	/**
	 * This method actually fills the request builder to avoid race conditions
	 *
	 * @param session the session of the current scenario
	 */
	protected def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): Validation[String, RequestBuilder] = {

		val url = {
			def makeAbsolute(url: String): Validation[String, String] = {

				if (url.startsWith(Protocol.HTTP.getProtocol))
					url.success
				else
					protocolConfiguration.baseURL.map(baseURL => (baseURL + url).success).getOrElse(("No protocolConfiguration.baseURL defined but provided url is relative : " + url).failure)
			}

			httpAttributes.url(session).flatMap(makeAbsolute)
		}

		def configureUrlCookiesAndProxy(requestBuilder: RequestBuilder)(url: String): Validation[String, RequestBuilder] = {

			val proxy = if (url.startsWith(Protocol.HTTPS.getProtocol))
				protocolConfiguration.securedProxy
			else protocolConfiguration.proxy

			proxy.map(requestBuilder.setProxyServer)

			for (cookie <- CookieHandling.getStoredCookies(session, url)) requestBuilder.addCookie(cookie)

			requestBuilder.setUrl(url).success
		}

		def configureQueryParams(requestBuilder: RequestBuilder): Validation[String, RequestBuilder] =
			HttpHelper.httpParamsToFluentMap(httpAttributes.queryParams, session).map(requestBuilder.setQueryParameters)

		def configureHeaders(requestBuilder: RequestBuilder): Validation[String, RequestBuilder] = {

			val resolvedHeaders = httpAttributes.headers.map {
				case (key, value) =>
					for {
						resolvedValue <- value(session)
					} yield key -> resolvedValue
			}
				.toList
				.flattenIt
				.map(_.toMap)

			resolvedHeaders.map { headers =>
				val newHeaders = RefererHandling.addStoredRefererHeader(protocolConfiguration.baseHeaders ++ headers, session, protocolConfiguration)
				newHeaders.foreach { case (headerName, headerValue) => requestBuilder.addHeader(headerName, headerValue) }
				requestBuilder
			}
		}

		def configureRealm(requestBuilder: RequestBuilder): Validation[String, RequestBuilder] =
			httpAttributes.realm match {
				case Some(realm) => realm(session).map(requestBuilder.setRealm)
				case None => requestBuilder.success
			}

		val requestBuilder = new RequestBuilder(httpAttributes.method, configuration.http.useRawUrl).setBodyEncoding(configuration.simulation.encoding)

		url
			.flatMap(configureUrlCookiesAndProxy(requestBuilder: RequestBuilder))
			.flatMap(configureQueryParams)
			.flatMap(configureHeaders)
			.flatMap(configureRealm)
	}

	/**
	 * This method builds the request that will be sent
	 *
	 * @param session the session of the current scenario
	 */
	private[http] def build(session: Session, protocolConfiguration: HttpProtocolConfiguration): Validation[String, Request] = getAHCRequestBuilder(session, protocolConfiguration).map(_.build)

	private[gatling] def toActionBuilder = HttpRequestActionBuilder(httpAttributes.requestName, this, httpAttributes.checks)
}
