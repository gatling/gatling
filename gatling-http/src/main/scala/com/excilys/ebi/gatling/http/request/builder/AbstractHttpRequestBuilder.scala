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

import com.ning.http.client.Request
import com.ning.http.client.RequestBuilder
import com.ning.http.client.FluentStringsMap
import com.ning.http.client.FluentCaseInsensitiveStringsMap
import org.fusesource.scalate._
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration._
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.request.HttpRequest
import scala.collection.immutable.HashMap
import com.ning.http.client.Realm
import com.ning.http.client.Realm.AuthScheme
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.ning.http.client.Response
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.ning.http.client.ProxyServer.Protocol

/**
 * AbstractHttpRequestBuilder class companion
 */
object AbstractHttpRequestBuilder {
	/**
	 * Implicit converter from requestBuilder to HttpRequestActionBuilder
	 *
	 * @param requestBuilder the request builder to convert
	 */
	implicit def toHttpRequestActionBuilder[B <: AbstractHttpRequestBuilder[B]](requestBuilder: B) = requestBuilder.httpRequestActionBuilder.withRequest(new HttpRequest(requestBuilder.httpRequestActionBuilder.requestName, requestBuilder))
}

/**
 * This class serves as model for all HttpRequestBuilders
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param urlFunction the function returning the url
 * @param queryParams the query parameters that should be added to the request
 * @param headers the headers that should be added to the request
 * @param followsRedirects sets the follow redirect option of AHC
 * @param credentials sets the credentials in case of Basic HTTP Authentication
 */
abstract class AbstractHttpRequestBuilder[B <: AbstractHttpRequestBuilder[B]](val httpRequestActionBuilder: HttpRequestActionBuilder, method: String, urlFunction: Session => String,
	queryParams: List[(Session => String, Session => String)], headers: Map[String, Session => String], followsRedirects: Option[Boolean], credentials: Option[(String, String)])
		extends CookieHandling with Logging {

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
	 * @param urlFunction the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param followsRedirects sets the follow redirect option of AHC
	 * @param credentials sets the credentials in case of Basic HTTP Authentication
	 */
	private[http] def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Session => String, queryParams: List[(Session => String, Session => String)], headers: Map[String, Session => String], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B

	/**
	 * Stops defining the request and adds checks on the response
	 *
	 * @param checkBuilders the checks that will be performed on the reponse
	 */
	def check(checks: HttpCheck[_]*): AbstractActionBuilder = httpRequestActionBuilder.withRequest(new HttpRequest(httpRequestActionBuilder.requestName, this)).withProcessors(checks)

	/**
	 * Adds a query parameter to the request
	 *
	 * @param paramKeyFunction a function that returns the key name
	 * @param paramValueFunction a function that returns the value
	 */
	def queryParam(paramKeyFunction: Session => String, paramValueFunction: Session => String): B = newInstance(httpRequestActionBuilder, urlFunction, (paramKeyFunction, paramValueFunction) :: queryParams, headers, followsRedirects, credentials)

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
	def header(header: (String, String)): B = newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers + (header._1 -> interpolate(header._2)), followsRedirects, credentials)

	/**
	 * Adds several headers to the request at the same time
	 *
	 * @param givenHeaders a scala map containing the headers to add
	 */
	def headers(givenHeaders: Map[String, String]): B = newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers ++ givenHeaders.mapValues(interpolate(_)), followsRedirects, credentials)

	/**
	 * Sets the follow redirect option that will be applied on AHC
	 *
	 * @param followRedirect a boolean that activates (true) or deactivates (false) the follow redirect option
	 */
	def followsRedirect(followRedirect: Boolean): B = newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers, Some(followRedirect), credentials)

	/**
	 * Adds Accept and Content-Type headers to the request set with "application/json" values
	 */
	def asJSON(): B = header(ACCEPT, APPLICATION_JSON).header(CONTENT_TYPE, APPLICATION_JSON)

	/**
	 * Adds Accept and Content-Type headers to the request set with "application/xml" values
	 */
	def asXML(): B = header(ACCEPT, APPLICATION_XML).header(CONTENT_TYPE, APPLICATION_XML)

	/**
	 * Adds BASIC authentication to the request
	 *
	 * @param username the username needed
	 * @param password the password needed
	 */
	def basicAuth(username: String, password: String): B = newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers, followsRedirects, Some((username, password)))

	/**
	 * This method actually fills the request builder to avoid race conditions
	 *
	 * @param session the session of the current scenario
	 */
	private[http] def getRequestBuilder(session: Session, protocolConfiguration: Option[HttpProtocolConfiguration]): RequestBuilder = {
		val requestBuilder = new RequestBuilder
		requestBuilder setMethod method setFollowRedirects followsRedirects.getOrElse(false)

		val isHttps = addURLAndCookiesTo(requestBuilder, session, protocolConfiguration)
		addProxyTo(requestBuilder, session, isHttps, protocolConfiguration)
		addQueryParamsTo(requestBuilder, session)
		addHeadersTo(requestBuilder, headers, session)
		addAuthenticationTo(requestBuilder, credentials)

		requestBuilder
	}

	/**
	 * This method builds the request that will be sent
	 *
	 * @param session the session of the current scenario
	 */
	private[http] def build(session: Session, protocolConfiguration: Option[HttpProtocolConfiguration]): Request = getRequestBuilder(session, protocolConfiguration).build

	/**
	 * This method adds proxy information to the request builder if needed
	 *
	 * @param requestBuilder the request builder to which the proxy should be added
	 * @param session the session of the current scenario
	 */
	private def addProxyTo(requestBuilder: RequestBuilder, session: Session, isHttps: Boolean, protocolConfiguration: Option[HttpProtocolConfiguration]) = {
		protocolConfiguration.map { httpConfiguration =>
			(if (isHttps) httpConfiguration.securedProxy else httpConfiguration.proxy).map(requestBuilder.setProxyServer(_))
		}
	}

	/**
	 * This method adds the url and cookies to the request builder. It does so by applying the urlFunction to the current session
	 *
	 * @param requestBuilder the request builder to which the url should be added
	 * @param session the session of the current scenario
	 */
	private def addURLAndCookiesTo(requestBuilder: RequestBuilder, session: Session, protocolConfiguration: Option[HttpProtocolConfiguration]) = {
		val providedUrl = urlFunction(session)

		// baseUrl implementation
		val url = if (providedUrl.startsWith(Protocol.HTTP.getProtocol))
			providedUrl
		else protocolConfiguration match {
			case Some(config) => config.baseURL.getOrElse(throw new IllegalArgumentException("No protocolConfiguration.baseURL defined but provided url is relative : " + providedUrl)) + providedUrl
			case None => throw new IllegalArgumentException("No protocolConfiguration defined but provided url is relative : " + providedUrl)
		}

		requestBuilder.setUrl(url)

		for (cookie <- getStoredCookies(session, url))
			requestBuilder.addCookie(cookie)

		url.startsWith(Protocol.HTTPS.getProtocol)
	}

	/**
	 * This method adds the query parameters to the request builder
	 *
	 * @param requestBuilder the request builder to which the query parameters should be added
	 * @param session the session of the current scenario
	 */
	private def addQueryParamsTo(requestBuilder: RequestBuilder, session: Session) = {
		val queryParamsMap = new FluentStringsMap

		val keyValues = for ((keyFunction, valueFunction) <- queryParams) yield (keyFunction(session), valueFunction(session))

		keyValues.groupBy(_._1).foreach { entry =>
			val (key, values) = entry
			queryParamsMap.add(key, values.map(_._2): _*)
		}

		requestBuilder.setQueryParameters(queryParamsMap)
	}

	/**
	 * This method adds the headers to the request builder
	 *
	 * @param requestBuilder the request builder to which the headers should be added
	 * @param session the session of the current scenario
	 */
	private def addHeadersTo(requestBuilder: RequestBuilder, headers: Map[String, Session => String], session: Session) = {
		requestBuilder setHeaders (new FluentCaseInsensitiveStringsMap)
		headers.foreach(header => requestBuilder addHeader (header._1, header._2(session)))
	}

	/**
	 * This method adds authentication to the request builder if needed
	 *
	 * @param requestBuilder the request builder to which the credentials should be added
	 * @param credentials the credentials to put in the request builder
	 */
	private def addAuthenticationTo(requestBuilder: RequestBuilder, credentials: Option[(String, String)]) = {
		credentials.map { c =>
			val (username, password) = c
			val realm = new Realm.RealmBuilder().setPrincipal(username).setPassword(password).setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC).build
			requestBuilder.setRealm(realm)
		}
	}
}