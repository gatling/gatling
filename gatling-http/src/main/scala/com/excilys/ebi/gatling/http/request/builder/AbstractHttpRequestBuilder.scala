/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration._
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.request.MIMEType._
import com.excilys.ebi.gatling.http.util.HttpHelper._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.ning.http.client.Cookie
import scala.collection.immutable.HashMap
import com.ning.http.client.Realm
import com.ning.http.client.Realm.AuthScheme
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder

object AbstractHttpRequestBuilder {
	implicit def toHttpRequestActionBuilder[B <: AbstractHttpRequestBuilder[B]](requestBuilder: B) = requestBuilder.httpRequestActionBuilder withRequest (new HttpRequest(requestBuilder.httpRequestActionBuilder.requestName, requestBuilder))
}

abstract class AbstractHttpRequestBuilder[B <: AbstractHttpRequestBuilder[B]](val httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
	headers: Option[Map[String, String]], followsRedirects: Option[Boolean], credentials: Option[(String, String)])
		extends Logging {

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], followsRedirects: Option[Boolean], credentials: Option[Tuple2[String, String]]): B

	def capture(captureBuilders: HttpCheckBuilder[_]*) = httpRequestActionBuilder withRequest (new HttpRequest(httpRequestActionBuilder.requestName, this)) withProcessors captureBuilders

	def check(checkBuilders: HttpCheckBuilder[_]*) = httpRequestActionBuilder withRequest (new HttpRequest(httpRequestActionBuilder.requestName, this)) withProcessors checkBuilders

	def queryParam(paramKey: String, paramValue: String): B = newInstance(httpRequestActionBuilder, urlFormatter, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, followsRedirects, credentials)

	def queryParam(paramKey: String, paramValue: FromContext): B = newInstance(httpRequestActionBuilder, urlFormatter, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, followsRedirects, credentials)

	def queryParam(paramKey: String): B = queryParam(paramKey, FromContext(paramKey))

	def header(header: Tuple2[String, String]): B = newInstance(httpRequestActionBuilder, urlFormatter, queryParams, Some(headers.get + (header._1 -> header._2)), followsRedirects, credentials)

	def headers(givenHeaders: Map[String, String]): B = newInstance(httpRequestActionBuilder, urlFormatter, queryParams, Some(headers.get ++ givenHeaders), followsRedirects, credentials)

	def followsRedirect(followRedirect: Boolean): B = newInstance(httpRequestActionBuilder, urlFormatter, queryParams, headers, Some(followRedirect), credentials)

	def asJSON(): B = newInstance(httpRequestActionBuilder, urlFormatter, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_JSON) + (CONTENT_TYPE -> APPLICATION_JSON)), followsRedirects, credentials)

	def asXML(): B = newInstance(httpRequestActionBuilder, urlFormatter, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_XML) + (CONTENT_TYPE -> APPLICATION_XML)), followsRedirects, credentials)

	def basicAuth(username: String, password: String): B = newInstance(httpRequestActionBuilder, urlFormatter, queryParams, headers, followsRedirects, Some((username, password)))

	def getMethod: String

	def getRequestBuilder(context: Context): RequestBuilder = {
		logger.debug("Building in HttpRequestBuilder")
		val requestBuilder = new RequestBuilder
		requestBuilder setMethod getMethod setFollowRedirects followsRedirects.getOrElse(false)

		addURLTo(requestBuilder, context)
		addProxyTo(requestBuilder, context)
		addCookiesTo(requestBuilder, context)
		addQueryParamsTo(requestBuilder, context)
		addHeadersTo(requestBuilder, headers)
		addAuthenticationTo(requestBuilder, credentials)

		requestBuilder
	}

	def build(context: Context): Request = {

		val request = getRequestBuilder(context) build

		logger.debug("Built {} Request: {})", getMethod, request.getCookies)
		request
	}

	private def addProxyTo(requestBuilder: RequestBuilder, context: Context) = {
		val httpConfiguration = context.getProtocolConfiguration(HTTP_PROTOCOL_TYPE).map { configuration =>
			configuration.asInstanceOf[HttpProtocolConfiguration]
		}

		if (httpConfiguration.isDefined)
			httpConfiguration.get.getProxy.map { proxy =>
				requestBuilder.setProxyServer(proxy)
				logger.debug("PROXY SET")
			}
	}

	private def addURLTo(requestBuilder: RequestBuilder, context: Context) = {
		val urlProvided = urlFormatter.get(context)

		val httpConfiguration = context.getProtocolConfiguration(HTTP_PROTOCOL_TYPE).map { configuration =>
			configuration.asInstanceOf[HttpProtocolConfiguration]
		}

		val url =
			if (urlProvided.startsWith("http"))
				urlProvided
			else if (httpConfiguration.isDefined)
				httpConfiguration.get.getBaseUrl.map {
					baseUrl => baseUrl + urlProvided
				}.getOrElse(urlProvided)
			else
				throw new IllegalArgumentException("URL is invalid (does not start with http): " + urlProvided)

		requestBuilder.setUrl(url)
	}

	private def addCookiesTo(requestBuilder: RequestBuilder, context: Context) = {
		logger.debug("Adding Cookies to RequestBuilder: {}", context.getAttributeAsOption(COOKIES_CONTEXT_KEY).getOrElse(Map.empty))
		for ((cookieName, cookie) <- context.getAttributeAsOption(COOKIES_CONTEXT_KEY).getOrElse(HashMap.empty).asInstanceOf[HashMap[String, Cookie]]) {
			logger.debug("Cookie added to request: {}", cookie)
			requestBuilder.addOrReplaceCookie(cookie)
		}
	}

	private def addQueryParamsTo(requestBuilder: RequestBuilder, context: Context) = {
		for (queryParam <- queryParams.get) {
			queryParam._2 match {
				case StringParam(string) =>
					requestBuilder addQueryParameter (queryParam._1, string)
				case ContextParam(string) =>
					requestBuilder addQueryParameter (queryParam._1, context.getAttribute(string).toString)
			}
		}
	}

	private def addHeadersTo(requestBuilder: RequestBuilder, headers: Option[Map[String, String]]) = {
		requestBuilder setHeaders (new FluentCaseInsensitiveStringsMap)
		for (header <- headers.get) { requestBuilder addHeader (header._1, header._2) }
	}

	private def addAuthenticationTo(requestBuilder: RequestBuilder, credentials: Option[Tuple2[String, String]]) = {
		credentials.map { c =>
			val (username, password) = c
			val realm = new Realm.RealmBuilder().setPrincipal(username).setPassword(password).setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC).build
			requestBuilder.setRealm(realm)
		}
	}
}