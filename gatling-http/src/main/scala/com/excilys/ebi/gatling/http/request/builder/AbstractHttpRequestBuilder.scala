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

import com.excilys.ebi.gatling.core.Predef.stringToSessionFunction
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.session.{ Session, EvaluatableString }
import com.excilys.ebi.gatling.core.util.StringHelper.{ parseEvaluatable, EL_START, EL_END }
import com.excilys.ebi.gatling.http.Headers.{ Values => HeaderValues, Names => HeaderNames }
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.http.referer.RefererHandling
import com.ning.http.client.{ FluentStringsMap, FluentCaseInsensitiveStringsMap, RequestBuilder, Request, Realm }
import com.ning.http.client.ProxyServer.Protocol
import com.ning.http.client.Realm.AuthScheme

/**
 * AbstractHttpRequestBuilder class companion
 */
object AbstractHttpRequestBuilder {

	/**
	 * Implicit converter from requestBuilder to HttpRequestActionBuilder
	 *
	 * @param requestBuilder the request builder to convert
	 */
	implicit def toActionBuilder(requestBuilder: AbstractHttpRequestBuilder[_]) = requestBuilder.toActionBuilder
}

/**
 * This class serves as model for all HttpRequestBuilders
 *
 * @param requestName is the name of the request
 * @param method is the HTTP method
 * @param url the function returning the url
 * @param queryParams the query parameters that should be added to the request
 * @param headers the headers that should be added to the request
 * @param realm is the session/realm the request should operate within
 * @param checks is a list of checks to execute on the response
 */
abstract class AbstractHttpRequestBuilder[B <: AbstractHttpRequestBuilder[B]](
	requestName: String,
	method: String,
	url: EvaluatableString,
	queryParams: List[HttpParam],
	headers: Map[String, EvaluatableString],
	realm: Option[Session => Realm],
	checks: List[HttpCheck[_]]) {

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param requestName is the name of the request
	 * @param url the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param credentials sets the credentials in case of Basic HTTP Authentication
	 */
	private[http] def newInstance(
		requestName: String,
		url: EvaluatableString,
		queryParams: List[HttpParam],
		headers: Map[String, EvaluatableString],
		credentials: Option[Session => Realm],
		checks: List[HttpCheck[_]]): B

	private[http] def withRequestName(requestName: String): B = newInstance(requestName, url, queryParams, headers, realm, checks)

	/**
	 * Stops defining the request and adds checks on the response
	 *
	 * @param checks the checks that will be performed on the response
	 */
	def check(checks: HttpCheck[_]*): B = newInstance(requestName, url, queryParams, headers, realm, this.checks ::: checks.toList)

	/**
	 * Adds a query parameter to the request
	 *
	 * @param param is a query parameter
	 */
	def queryParam(param: HttpParam): B = newInstance(requestName, url, param :: queryParams, headers, realm, checks)

	/**
	 * Adds a query parameter to the request
	 *
	 * The value is a session attribute with the same key
	 *
	 * @param paramKey the key of the parameter
	 */
	def queryParam(paramKey: String): B = queryParam(paramKey, EL_START + paramKey + EL_END)

	/**
	 * Adds a header to the request
	 *
	 * @param header the header to add, eg: ("Content-Type", "application/json")
	 */
	def header(header: (String, String)): B = newInstance(requestName, url, queryParams, headers + (header._1 -> parseEvaluatable(header._2)), realm, checks)

	/**
	 * Adds several headers to the request at the same time
	 *
	 * @param givenHeaders a scala map containing the headers to add
	 */
	def headers(givenHeaders: Map[String, String]): B = newInstance(requestName, url, queryParams, headers ++ givenHeaders.mapValues(parseEvaluatable(_)), realm, checks)

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
	def basicAuth(username: EvaluatableString, password: EvaluatableString): B = {
		val buildRealm = (session: Session) => new Realm.RealmBuilder().setPrincipal(username(session)).setPassword(password(session)).setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC).build
		newInstance(requestName, url, queryParams, headers, Some(buildRealm), checks)
	}

	/**
	 * This method actually fills the request builder to avoid race conditions
	 *
	 * @param session the session of the current scenario
	 */
	protected def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): RequestBuilder = {
		val requestBuilder = new RequestBuilder(method, configuration.http.userRawUrl).setBodyEncoding(configuration.simulation.encoding)

		val isHttps = configureURLAndCookies(requestBuilder, session, protocolConfiguration)
		configureProxy(requestBuilder, session, isHttps, protocolConfiguration)
		configureQueryParams(requestBuilder, session)
		configureHeaders(requestBuilder, headers, session, protocolConfiguration)
		configureRealm(requestBuilder, realm, session)

		requestBuilder
	}

	/**
	 * This method builds the request that will be sent
	 *
	 * @param session the session of the current scenario
	 */
	private[http] def build(session: Session, protocolConfiguration: HttpProtocolConfiguration): Request = getAHCRequestBuilder(session, protocolConfiguration).build

	/**
	 * This method adds proxy information to the request builder if needed
	 *
	 * @param requestBuilder the request builder to which the proxy should be added
	 * @param session the session of the current scenario
	 */
	private def configureProxy(requestBuilder: RequestBuilder, session: Session, isHttps: Boolean, protocolConfiguration: HttpProtocolConfiguration) = {
			(if (isHttps)
				protocolConfiguration.securedProxy
			else
				protocolConfiguration.proxy).map(requestBuilder.setProxyServer(_))
	}

	/**
	 * This method adds the url and cookies to the request builder. It does so by applying the url to the current session
	 *
	 * @param requestBuilder the request builder to which the url should be added
	 * @param session the session of the current scenario
	 */
	private def configureURLAndCookies(requestBuilder: RequestBuilder, session: Session, protocolConfiguration: HttpProtocolConfiguration) = {
		val providedUrl = url(session)

		// baseUrl implementation
		val resolvedUrl = if (providedUrl.startsWith(Protocol.HTTP.getProtocol))
			providedUrl
		else 
			protocolConfiguration.baseURL.getOrElse(throw new IllegalArgumentException("No protocolConfiguration.baseURL defined but provided url is relative : " + providedUrl)) + providedUrl

		requestBuilder.setUrl(resolvedUrl)

		for (cookie <- CookieHandling.getStoredCookies(session, resolvedUrl))
			requestBuilder.addCookie(cookie)

		resolvedUrl.startsWith(Protocol.HTTPS.getProtocol)
	}

	/**
	 * This method adds the query parameters to the request builder
	 *
	 * @param requestBuilder the request builder to which the query parameters should be added
	 * @param session the session of the current scenario
	 */
	private def configureQueryParams(requestBuilder: RequestBuilder, session: Session) {

		if (!queryParams.isEmpty) {

			val queryParamsMap = new FluentStringsMap

			queryParams.map { case (keyFunction, valueFunction) => (keyFunction(session), valueFunction(session)) }
				.groupBy(_._1)
				.foreach {
					case (key, values) => queryParamsMap.add(key, values.map(_._2): _*)
				}

			requestBuilder.setQueryParameters(queryParamsMap)
		}
	}

	/**
	 * This method adds the headers to the request builder
	 *
	 * @param requestBuilder the request builder to which the headers should be added
	 * @param session the session of the current scenario
	 */
	private def configureHeaders(requestBuilder: RequestBuilder, headers: Map[String, EvaluatableString], session: Session, protocolConfiguration: HttpProtocolConfiguration) {
		requestBuilder.setHeaders(new FluentCaseInsensitiveStringsMap)

		val baseHeaders = protocolConfiguration.baseHeaders
		val resolvedRequestHeaders = headers.map { case (headerName, headerValue) => (headerName -> headerValue(session)) }

		val newHeaders = RefererHandling.addStoredRefererHeader(baseHeaders ++ resolvedRequestHeaders, session, protocolConfiguration)
		newHeaders.foreach { case (headerName, headerValue) => requestBuilder.addHeader(headerName, headerValue) }
	}

	/**
	 * This method adds authentication to the request builder if needed
	 *
	 * @param requestBuilder the request builder to which the credentials should be added
	 * @param realm the credentials to put in the request builder
	 * @param session the session of the current scenario
	 */
	private def configureRealm(requestBuilder: RequestBuilder, realm: Option[Session => Realm], session: Session) {
		realm.map { realm => requestBuilder.setRealm(realm(session)) }
	}

	private[gatling] def toActionBuilder = new HttpRequestActionBuilder(requestName, this, null, checks)
}
