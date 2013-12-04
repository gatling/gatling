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
package com.excilys.ebi.gatling.http.request.builder

import org.fusesource.scalate.{ Binding, TemplateEngine }
import org.fusesource.scalate.support.ScalaCompiler

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.session.{ EvaluatableString, EvaluatableStringSeq, Session }
import com.excilys.ebi.gatling.core.session.ELParser.parseEL
import com.excilys.ebi.gatling.core.session.Session.{ attributeAsEvaluatableString, attributeAsEvaluatableStringSeq, evaluatableStringToEvaluatableStringSeq }
import com.excilys.ebi.gatling.core.util.FileHelper.SSP_EXTENSION
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames }
import com.excilys.ebi.gatling.http.Headers.Names.CONTENT_LENGTH
import com.excilys.ebi.gatling.http.Headers.{ Values => HeaderValues }
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.ahc.GatlingConnectionPoolKeyStrategy
import com.excilys.ebi.gatling.http.cache.CacheHandling
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.http.referer.RefererHandling
import com.excilys.ebi.gatling.http.request.{ ByteArrayBody, FilePathBody, HttpRequestBody, SessionByteArrayBody, StringBody, TemplateBody }
import com.excilys.ebi.gatling.http.util.HttpHelper.httpParamsToFluentMap
import com.ning.http.client.{ Request, RequestBuilder }
import com.ning.http.client.ProxyServer.Protocol
import com.ning.http.client.Realm
import com.ning.http.client.Realm.AuthScheme

case class HttpAttributes(
	requestName: EvaluatableString,
	method: String,
	url: EvaluatableString,
	queryParams: List[HttpParam],
	headers: Map[String, EvaluatableString],
	realm: Option[Session => Realm],
	virtualHost: Option[String],
	checks: List[HttpCheck[_]],
	body: Option[HttpRequestBody])

object AbstractHttpRequestBuilder {
	val TEMPLATE_ENGINE = {
		val engine = new TemplateEngine(List(GatlingFiles.requestBodiesDirectory.jfile))
		engine.allowReload = false
		engine.escapeMarkup = false
		system.registerOnTermination(engine.compiler.asInstanceOf[ScalaCompiler].compiler.askShutdown)
		engine
	}
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
	def check(checks: HttpCheck[_]*): B = newInstance(httpAttributes.copy(checks = httpAttributes.checks ::: checks.toList))

	/**
	 * Adds a query parameter to the request
	 *
	 * The value is a session attribute with the same key
	 *
	 * @param key the key of the parameter
	 */
	def queryParam(key: String): B = queryParam(parseEL(key), attributeAsEvaluatableString(key))

	/**
	 * Adds a query parameter to the request
	 *
	 * @param param is a query parameter
	 */
	def queryParam(key: EvaluatableString, value: EvaluatableString): B = {
		val httpParam: HttpParam = (key, evaluatableStringToEvaluatableStringSeq(value))
		queryParam(httpParam)
	}

	def multiValuedQueryParam(key: String): B = multiValuedQueryParam(parseEL(key), key)

	def multiValuedQueryParam(key: EvaluatableString, value: String): B = {
		val httpParam: HttpParam = (key, attributeAsEvaluatableStringSeq(value))
		queryParam(httpParam)
	}

	def multiValuedQueryParam(key: EvaluatableString, values: Seq[String]): B = {
		val httpParam: HttpParam = (key, (s: Session) => values)
		queryParam(httpParam)
	}

	def multiValuedQueryParam(key: EvaluatableString, values: EvaluatableStringSeq): B = {
		val httpParam: HttpParam = (key, values)
		queryParam(httpParam)
	}

	private def queryParam(param: HttpParam): B = newInstance(httpAttributes.copy(queryParams = param :: httpAttributes.queryParams))

	/**
	 * Adds a header to the request
	 *
	 * @param header the header to add, eg: ("Content-Type", "application/json")
	 */
	def header(header: (String, String)): B = newInstance(httpAttributes.copy(headers = httpAttributes.headers + (header._1 -> parseEL(header._2))))

	/**
	 * Adds several headers to the request at the same time
	 *
	 * @param givenHeaders a scala map containing the headers to add
	 */
	def headers(givenHeaders: Map[String, String]): B = newInstance(httpAttributes.copy(headers = httpAttributes.headers ++ givenHeaders.mapValues(parseEL)))

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
		newInstance(httpAttributes.copy(realm = Some(buildRealm)))
	}

	/**
	 * @param virtualHost a virtual host to override default computed one
	 */
	def virtualHost(virtualHost: String): B = newInstance(httpAttributes.copy(virtualHost = Some(virtualHost)))

	/**
	 * This method actually fills the request builder to avoid race conditions
	 *
	 * @param session the session of the current scenario
	 */
	protected def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): RequestBuilder = {
		val requestBuilder = new RequestBuilder(httpAttributes.method, configuration.http.useRawUrl).setBodyEncoding(configuration.core.encoding)

		if (!protocolConfiguration.shareConnections) requestBuilder.setConnectionPoolKeyStrategy(new GatlingConnectionPoolKeyStrategy(session))

		val isHttps = configureQueryAndCookies(requestBuilder, session, protocolConfiguration)
		configureProxy(requestBuilder, session, isHttps, protocolConfiguration)
		configureHeaders(requestBuilder, httpAttributes.headers, session, protocolConfiguration)
		configureRealm(requestBuilder, httpAttributes.realm, session)
		configureVirtualHost(requestBuilder, protocolConfiguration)
		configureBody(requestBuilder, httpAttributes.body, session)

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
			protocolConfiguration.proxy).map(requestBuilder.setProxyServer)
	}

	private def configureVirtualHost(requestBuilder: RequestBuilder, protocolConfiguration: HttpProtocolConfiguration) = {

		val virtualHost = httpAttributes.virtualHost.orElse(protocolConfiguration.virtualHost)
		virtualHost.map(requestBuilder.setVirtualHost)
	}

	/**
	 * This method adds the url and cookies to the request builder. It does so by applying the url to the current session
	 *
	 * @param requestBuilder the request builder to which the url should be added
	 * @param session the session of the current scenario
	 */
	private def configureQueryAndCookies(requestBuilder: RequestBuilder, session: Session, protocolConfiguration: HttpProtocolConfiguration) = {
		val providedUrl = httpAttributes.url(session)

		// baseUrl implementation
		val resolvedUrl = if (providedUrl.startsWith(Protocol.HTTP.getProtocol))
			providedUrl
		else
			protocolConfiguration.baseURL.getOrElse(throw new IllegalArgumentException("No protocolConfiguration.baseURL defined but provided url is relative : " + providedUrl)) + providedUrl

		if (!httpAttributes.queryParams.isEmpty) {
			val queryParamsMap = httpParamsToFluentMap(httpAttributes.queryParams, session)
			requestBuilder.setQueryParameters(queryParamsMap)
		}

		requestBuilder.setUrl(resolvedUrl)

		CacheHandling.getLastModified(protocolConfiguration, session, resolvedUrl).foreach(requestBuilder.setHeader(HeaderNames.IF_MODIFIED_SINCE, _))
		CacheHandling.getEtag(protocolConfiguration, session, resolvedUrl).foreach(requestBuilder.setHeader(HeaderNames.IF_NONE_MATCH, _))

		for (cookie <- CookieHandling.getStoredCookies(session, resolvedUrl))
			requestBuilder.addCookie(cookie)

		resolvedUrl.startsWith(Protocol.HTTPS.getProtocol)
	}

	/**
	 * This method adds the headers to the request builder
	 *
	 * @param requestBuilder the request builder to which the headers should be added
	 * @param session the session of the current scenario
	 */
	private def configureHeaders(requestBuilder: RequestBuilder, headers: Map[String, EvaluatableString], session: Session, protocolConfiguration: HttpProtocolConfiguration) {

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

	/**
	 * Adds a body to the request
	 *
	 * @param body a string containing the body of the request
	 */
	def body(body: EvaluatableString): B = newInstance(httpAttributes.copy(body = Some(StringBody(body))))

	/**
	 * Adds a body from a file to the request
	 *
	 * @param filePath the path of the file relative to directory containing the templates
	 */
	def fileBody(filePath: String): B = newInstance(httpAttributes.copy(body = Some(FilePathBody(filePath))))

	/**
	 * Adds a body from a template that has to be compiled
	 *
	 * @param tplPath the path to the template relative to GATLING_TEMPLATES_FOLDER
	 * @param values the values that should be merged into the template
	 */
	def fileBody(tplPath: String, values: Map[String, EvaluatableString]): B = newInstance(httpAttributes.copy(body = Some(TemplateBody(tplPath, values))))

	/**
	 * Adds a body from a byteArray Session function to the request
	 *
	 * @param byteArray - The callback function which returns the ByteArray from which to build the body
	 */
	def byteArrayBody(byteArray: (Session) => Array[Byte]): B = newInstance(httpAttributes.copy(body = Some(SessionByteArrayBody(byteArray))))

	/**
	 * This method adds the body to the request builder
	 *
	 * @param requestBuilder the request builder to which the body should be added
	 * @param body the body that should be added
	 * @param session the session of the current scenario
	 */
	private def configureBody(requestBuilder: RequestBuilder, body: Option[HttpRequestBody], session: Session) {

		val contentLength = body.map {
			_ match {
				case FilePathBody(filePath) =>
					val file = (GatlingFiles.requestBodiesDirectory / filePath).jfile
					requestBuilder.setBody(file)
					file.length

				case StringBody(string) =>
					val body = string(session)
					requestBuilder.setBody(body)
					body.length

				case TemplateBody(tplPath, values) =>
					val body = compileBody(tplPath, values, session)
					requestBuilder.setBody(body)
					body.length

				case ByteArrayBody(byteArray) =>
					val body = byteArray()
					requestBuilder.setBody(body)
					body.length

				case SessionByteArrayBody(byteArray) =>
					val body = byteArray(session)
					requestBuilder.setBody(body)
					body.length
			}
		}

		contentLength.map(length => requestBuilder.setHeader(CONTENT_LENGTH, length.toString))
	}

	/**
	 * This method compiles the template for a TemplateBody
	 *
	 * @param tplPath the path to the template relative to GATLING_TEMPLATES_FOLDER
	 * @param params the params that should be merged into the template
	 * @param session the session of the current scenario
	 */
	private def compileBody(tplPath: String, params: Map[String, EvaluatableString], session: Session): String = {

		val bindings = for ((key, _) <- params) yield Binding(key, "String")
		val templateValues = for ((key, value) <- params) yield (key -> (value(session)))

		AbstractHttpRequestBuilder.TEMPLATE_ENGINE.layout(tplPath + SSP_EXTENSION, templateValues, bindings)
	}

	private[gatling] def toActionBuilder = HttpRequestActionBuilder(httpAttributes.requestName, this, httpAttributes.checks)
}

object HttpRequestBuilder {

	def apply(method: String, requestName: EvaluatableString, url: EvaluatableString) = new HttpRequestBuilder(HttpAttributes(requestName, method, url, Nil, Map.empty, None, None, Nil, None))
}

/**
 * This class defines an HTTP request with word GET in the DSL
 */
class HttpRequestBuilder(httpAttributes: HttpAttributes) extends AbstractHttpRequestBuilder[HttpRequestBuilder](httpAttributes) {

	private[http] def newInstance(httpAttributes: HttpAttributes) = new HttpRequestBuilder(httpAttributes)
}
