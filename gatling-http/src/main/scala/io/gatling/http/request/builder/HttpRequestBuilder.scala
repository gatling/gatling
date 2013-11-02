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
package io.gatling.http.request.builder

import java.net.{ InetAddress, URI }

import com.ning.http.client.{ Realm, RequestBuilder }
import com.ning.http.client.ProxyServer
import com.ning.http.client.ProxyServer.Protocol
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.Proxy
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.session.el.EL
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.action.HttpRequestActionBuilder
import io.gatling.http.ahc.{ ConnectionPoolKeyStrategy, ProxyConverter }
import io.gatling.http.cache.CacheHandling
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckOrder.Status
import io.gatling.http.config.HttpProtocol
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.referer.RefererHandling
import io.gatling.http.request.HttpRequest
import io.gatling.http.response.ResponseTransformer
import io.gatling.http.util.HttpHelper

case class HttpAttributes(
	requestName: Expression[String],
	method: String,
	urlOrURI: Either[Expression[String], URI],
	queryParams: List[HttpParam] = Nil,
	headers: Map[String, Expression[String]] = Map.empty,
	realm: Option[Expression[Realm]] = None,
	virtualHost: Option[Expression[String]] = None,
	address: Option[InetAddress] = None,
	checks: List[HttpCheck] = Nil,
	ignoreDefaultChecks: Boolean = false,
	responseTransformer: Option[ResponseTransformer] = None,
	maxRedirects: Option[Int] = None,
	useRawUrl: Boolean = false,
	proxy: Option[ProxyServer] = None,
	secureProxy: Option[ProxyServer] = None)

object AbstractHttpRequestBuilder {

	val jsonHeaderValueExpression = HeaderValues.APPLICATION_JSON.el[String]
	val xmlHeaderValueExpression = HeaderValues.APPLICATION_XML.el[String]
	val multipartFormDataValueExpression = HeaderValues.MULTIPART_FORM_DATA.el[String]
	val emptyHeaderListSuccess = List.empty[(String, String)].success

	implicit def toActionBuilder(requestBuilder: AbstractHttpRequestBuilder[_]) = new HttpRequestActionBuilder(requestBuilder)
}

/**
 * This class serves as model for all HttpRequestBuilders
 *
 * @param httpAttributes the base HTTP attributes
 */
abstract class AbstractHttpRequestBuilder[B <: AbstractHttpRequestBuilder[B]](val httpAttributes: HttpAttributes) extends Logging {

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
	 * Ignore the default checks configured on HttpProtocol
	 */
	def ignoreDefaultChecks: B = newInstance(httpAttributes.copy(ignoreDefaultChecks = true))

	def queryParam(key: Expression[String], value: Expression[Any]): B = queryParam(SimpleParam(key, value))
	def multivaluedQueryParam(key: Expression[String], values: Expression[Seq[Any]]): B = queryParam(MultivaluedParam(key, values))
	def queryParamsSequence(seq: Expression[Seq[(String, Any)]]): B = queryParam(ParamSeq(seq))
	def queryParamsMap(map: Expression[Map[String, Any]]): B = queryParam(ParamMap(map))
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
	def headers(newHeaders: Map[String, String]): B = newInstance(httpAttributes.copy(headers = httpAttributes.headers ++ newHeaders.mapValues(_.el[String])))

	/**
	 * Adds Accept and Content-Type headers to the request set with "application/json" values
	 */
	def asJSON: B = header(HeaderNames.ACCEPT, AbstractHttpRequestBuilder.jsonHeaderValueExpression).header(HeaderNames.CONTENT_TYPE, AbstractHttpRequestBuilder.jsonHeaderValueExpression)

	/**
	 * Adds Accept and Content-Type headers to the request set with "application/xml" values
	 */
	def asXML: B = header(HeaderNames.ACCEPT, AbstractHttpRequestBuilder.xmlHeaderValueExpression).header(HeaderNames.CONTENT_TYPE, AbstractHttpRequestBuilder.xmlHeaderValueExpression)

	/**
	 * Adds Content-Type header to the request set with "multipart/form-data" value
	 */
	def asMultipartForm: B = header(HeaderNames.CONTENT_TYPE, AbstractHttpRequestBuilder.multipartFormDataValueExpression)

	/**
	 * Adds BASIC authentication to the request
	 *
	 * @param username the username needed
	 * @param password the password needed
	 */
	def basicAuth(username: Expression[String], password: Expression[String]): B = newInstance(httpAttributes.copy(realm = Some(HttpHelper.buildRealm(username, password))))

	/**
	 * @param virtualHost a virtual host to override default compute one
	 */
	def virtualHost(virtualHost: Expression[String]): B = newInstance(httpAttributes.copy(virtualHost = Some(virtualHost)))

	def address(address: InetAddress): B = newInstance(httpAttributes.copy(address = Some(address)))

	/**
	 * @param responseTransformer transforms the response before it's handled to the checks pipeline
	 */
	def transformResponse(responseTransformer: ResponseTransformer): B = newInstance(httpAttributes.copy(responseTransformer = Some(responseTransformer)))

	def maxRedirects(max: Int): B = newInstance(httpAttributes.copy(maxRedirects = Some(max)))

	def useRawUrl: B = newInstance(httpAttributes.copy(useRawUrl = true))

	def proxy(httpProxy: Proxy): B = newInstance(httpAttributes.copy(proxy = Some(httpProxy.proxyServer), secureProxy = httpProxy.secureProxyServer))

	/**
	 * This method actually fills the request builder to avoid race conditions
	 *
	 * @param session the session of the current scenario
	 */
	protected def getAHCRequestBuilder(session: Session, protocol: HttpProtocol): Validation[RequestBuilder] = {

		def buildURI(urlOrURI: Either[Expression[String], URI]): Validation[URI] = {

			def makeAbsolute(url: String): Validation[String] =
				if (url.startsWith(Protocol.HTTP.getProtocol))
					url.success
				else
					protocol.baseURL.map(baseURL => (baseURL + url).success).getOrElse(s"No protocol.baseURL defined but provided url is relative : $url".failure)

			def createURI(url: String): Validation[URI] =
				try {
					URI.create(url).success
				} catch {
					case e: Exception => s"url $url can't be parsed into a URI: ${e.getMessage}".failure
				}

			urlOrURI match {
				case Left(url) => url(session).flatMap(makeAbsolute).flatMap(createURI)
				case Right(uri) => uri.success
			}
		}

		def configureQueryCookiesAndProxy(uri: URI)(implicit requestBuilder: RequestBuilder): Validation[RequestBuilder] = {

			if (!protocol.proxyExceptions.contains(uri.getHost)) {
				if (uri.getScheme == Protocol.HTTP.getProtocol)
					httpAttributes.proxy.orElse(protocol.proxy).foreach(requestBuilder.setProxyServer)
				else
					httpAttributes.secureProxy.orElse(protocol.secureProxy).foreach(requestBuilder.setProxyServer)
			}

			protocol.localAddress.foreach(requestBuilder.setLocalInetAddress)
			httpAttributes.address.foreach(requestBuilder.setInetAddress)

			CookieHandling.getStoredCookies(session, uri).foreach(requestBuilder.addCookie)

			CacheHandling.getLastModified(protocol, session, uri).foreach(requestBuilder.setHeader(HeaderNames.IF_MODIFIED_SINCE, _))
			CacheHandling.getEtag(protocol, session, uri).foreach(requestBuilder.setHeader(HeaderNames.IF_NONE_MATCH, _))

			if (!httpAttributes.queryParams.isEmpty)
				HttpHelper.httpParamsToFluentMap(httpAttributes.queryParams, session).map(requestBuilder.setQueryParameters(_).setURI(uri))
			else
				requestBuilder.setURI(uri).success
		}

		def configureVirtualHost(requestBuilder: RequestBuilder): Validation[RequestBuilder] = {
			val virtualHost = httpAttributes.virtualHost.orElse(protocol.virtualHost)
			virtualHost.map(_(session).map(requestBuilder.setVirtualHost)).getOrElse(requestBuilder.success)
		}

		def configureHeaders(requestBuilder: RequestBuilder): Validation[RequestBuilder] = {

			val resolvedHeaders = httpAttributes.headers.foldLeft(AbstractHttpRequestBuilder.emptyHeaderListSuccess) { (headers, header) =>
				val (key, value) = header
				for {
					headers <- headers
					value <- value(session)
				} yield ((key -> value) :: headers)
			}

			resolvedHeaders.map { headers =>
				val newHeaders = RefererHandling.addStoredRefererHeader(protocol.baseHeaders ++ headers.reverse, session, protocol)
				newHeaders.foreach { case (headerName, headerValue) => requestBuilder.addHeader(headerName, headerValue) }
				requestBuilder
			}
		}

		def configureRealm(requestBuilder: RequestBuilder): Validation[RequestBuilder] = {

			val realm = httpAttributes.realm.orElse(protocol.basicAuth)

			realm match {
				case Some(realm) => realm(session).map(requestBuilder.setRealm)
				case None => requestBuilder.success
			}
		}

		implicit val requestBuilder = new RequestBuilder(httpAttributes.method, configuration.http.ahc.useRawUrl).setBodyEncoding(configuration.core.encoding)

		if (!protocol.shareConnections) requestBuilder.setConnectionPoolKeyStrategy(new ConnectionPoolKeyStrategy(session))

		buildURI(httpAttributes.urlOrURI)
			.flatMap(configureQueryCookiesAndProxy)
			.flatMap(configureVirtualHost)
			.flatMap(configureHeaders)
			.flatMap(configureRealm)
	}

	/**
	 * This method builds the request that will be sent
	 *
	 * @param session the session of the current scenario
	 */
	def build(protocol: HttpProtocol, throttled: Boolean): HttpRequest = {

		val ahcRequest = (session: Session) =>
			try {
				getAHCRequestBuilder(session, protocol).map(_.build)
			} catch {
				case e: Exception =>
					logger.warn("Failed to build request", e)
					s"Failed to build request: ${e.getMessage}".failure
			}

		val totalChecks = if (httpAttributes.ignoreDefaultChecks)
			httpAttributes.checks
		else
			protocol.checks ::: httpAttributes.checks

		val resolvedChecks = totalChecks
			.find(_.order == Status)
			.map(_ => httpAttributes.checks)
			.getOrElse(HttpRequestActionBuilder.defaultHttpCheck :: totalChecks)
			.sorted

		val resolvedMaxRedirects = httpAttributes.maxRedirects.orElse(protocol.maxRedirects)

		HttpRequest(
			httpAttributes.requestName,
			ahcRequest,
			totalChecks,
			httpAttributes.responseTransformer,
			httpAttributes.maxRedirects,
			throttled,
			protocol)
	}
}

object HttpRequestBuilder {

	def apply(method: String, requestName: Expression[String], urlOrURI: Either[Expression[String], URI]) = new HttpRequestBuilder(HttpAttributes(requestName, method, urlOrURI))
}

class HttpRequestBuilder(httpAttributes: HttpAttributes) extends AbstractHttpRequestBuilder[HttpRequestBuilder](httpAttributes) {

	private[http] def newInstance(httpAttributes: HttpAttributes) = new HttpRequestBuilder(httpAttributes)
}
