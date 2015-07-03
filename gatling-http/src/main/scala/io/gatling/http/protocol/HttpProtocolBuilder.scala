/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.protocol

import java.net.InetAddress

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.{ BlackList, Filters, WhiteList }
import io.gatling.core.session._
import io.gatling.core.session.el.El
import io.gatling.http.HeaderNames._
import io.gatling.http.ahc.ProxyConverter
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.ExtraInfoExtractor
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper

import org.asynchttpclient.{ RequestBuilderBase, Realm, Request, SignatureCalculator }
import org.asynchttpclient.uri.Uri

/**
 * HttpProtocolBuilder class companion
 */
object HttpProtocolBuilder {

  implicit def toHttpProtocol(builder: HttpProtocolBuilder): HttpProtocol = builder.build

  def apply(configuration: GatlingConfiguration): HttpProtocolBuilder =
    HttpProtocolBuilder(HttpProtocol(configuration))
}

/**
 * Builder for HttpProtocol used in DSL
 *
 * @param protocol the protocol being built
 */
case class HttpProtocolBuilder(protocol: HttpProtocol) {

  def baseURL(url: String) = copy(protocol = protocol.copy(baseURLs = List(Uri.create(url))))
  def baseURLs(urls: String*): HttpProtocolBuilder = baseURLs(urls.toList)
  def baseURLs(urls: List[String]): HttpProtocolBuilder = copy(protocol = protocol.copy(baseURLs = urls.map(Uri.create)))
  def warmUp(url: String): HttpProtocolBuilder = copy(protocol = copy(protocol.copy(warmUpUrl = Some(url))))
  def disableWarmUp: HttpProtocolBuilder = copy(protocol = protocol.copy(warmUpUrl = None))

  // enginePart
  private def newEnginePart(enginePart: HttpProtocolEnginePart) = copy(protocol = copy(protocol.copy(enginePart = enginePart)))
  def disableClientSharing = newEnginePart(protocol.enginePart.copy(shareClient = false))
  def shareConnections = newEnginePart(protocol.enginePart.copy(shareConnections = true))
  def shareDnsCache = newEnginePart(protocol.enginePart.copy(shareDnsCache = true))
  def virtualHost(virtualHost: Expression[String]) = newEnginePart(protocol.enginePart.copy(virtualHost = Some(virtualHost)))
  def localAddress(localAddress: Expression[InetAddress]) = newEnginePart(protocol.enginePart.copy(localAddress = Some(localAddress)))
  def maxConnectionsPerHostLikeFirefoxOld = maxConnectionsPerHost(2)
  def maxConnectionsPerHostLikeFirefox = maxConnectionsPerHost(6)
  def maxConnectionsPerHostLikeOperaOld = maxConnectionsPerHost(4)
  def maxConnectionsPerHostLikeOpera = maxConnectionsPerHost(6)
  def maxConnectionsPerHostLikeSafariOld = maxConnectionsPerHost(4)
  def maxConnectionsPerHostLikeSafari = maxConnectionsPerHost(6)
  def maxConnectionsPerHostLikeIE7 = maxConnectionsPerHost(2)
  def maxConnectionsPerHostLikeIE8 = maxConnectionsPerHost(6)
  def maxConnectionsPerHostLikeIE10 = maxConnectionsPerHost(8)
  def maxConnectionsPerHostLikeChrome = maxConnectionsPerHost(6)
  def maxConnectionsPerHost(max: Int): HttpProtocolBuilder = newEnginePart(protocol.enginePart.copy(maxConnectionsPerHost = max))

  // requestPart
  private def newRequestPart(requestPart: HttpProtocolRequestPart) = copy(protocol = copy(protocol.copy(requestPart = requestPart)))
  def disableAutoReferer = newRequestPart(protocol.requestPart.copy(autoReferer = false))
  def disableCaching = newRequestPart(protocol.requestPart.copy(cache = false))
  def header(name: String, value: Expression[String]) = newRequestPart(protocol.requestPart.copy(headers = protocol.requestPart.headers + (name -> value)))
  def headers(headers: Map[String, String]) = newRequestPart(protocol.requestPart.copy(headers = protocol.requestPart.headers ++ headers.mapValues(_.el[String])))
  def acceptHeader(value: Expression[String]) = header(Accept, value)
  def acceptCharsetHeader(value: Expression[String]) = header(AcceptCharset, value)
  def acceptEncodingHeader(value: Expression[String]) = header(AcceptEncoding, value)
  def acceptLanguageHeader(value: Expression[String]) = header(AcceptLanguage, value)
  def authorizationHeader(value: Expression[String]) = header(Authorization, value)
  def connectionHeader(value: Expression[String]) = header(Connection, value)
  def contentTypeHeader(value: Expression[String]) = header(ContentType, value)
  def doNotTrackHeader(value: Expression[String]) = header(DNT, value)
  def userAgentHeader(value: Expression[String]) = header(UserAgent, value)
  def basicAuth(username: Expression[String], password: Expression[String]) = authRealm(HttpHelper.buildBasicAuthRealm(username, password))
  def digestAuth(username: Expression[String], password: Expression[String]) = authRealm(HttpHelper.buildDigestAuthRealm(username, password))
  def ntlmAuth(username: Expression[String], password: Expression[String], ntlmDomain: Expression[String], ntlmHost: Expression[String]) = authRealm(HttpHelper.buildNTLMAuthRealm(username, password, ntlmDomain, ntlmHost))
  def authRealm(realm: Expression[Realm]) = newRequestPart(protocol.requestPart.copy(realm = Some(realm)))
  def silentResources = newRequestPart(protocol.requestPart.copy(silentResources = true))
  def silentURI(regex: String) = newRequestPart(protocol.requestPart.copy(silentURI = Some(regex.r.pattern)))
  def disableUrlEncoding = newRequestPart(protocol.requestPart.copy(disableUrlEncoding = true))
  def signatureCalculator(calculator: Expression[SignatureCalculator]): HttpProtocolBuilder = newRequestPart(protocol.requestPart.copy(signatureCalculator = Some(calculator)))
  def signatureCalculator(calculator: SignatureCalculator): HttpProtocolBuilder = signatureCalculator(calculator.expressionSuccess)
  def signatureCalculator(calculator: (Request, RequestBuilderBase[_]) => Unit): HttpProtocolBuilder = signatureCalculator(new SignatureCalculator {
    def calculateAndAddSignature(request: Request, requestBuilder: RequestBuilderBase[_]): Unit = calculator(request, requestBuilder)
  })

  // responsePart
  private def newResponsePart(responsePart: HttpProtocolResponsePart) = copy(protocol = copy(protocol.copy(responsePart = responsePart)))
  def disableFollowRedirect = newResponsePart(protocol.responsePart.copy(followRedirect = false))
  def maxRedirects(max: Int) = newResponsePart(protocol.responsePart.copy(maxRedirects = Some(max)))
  def strict302Handling = newResponsePart(protocol.responsePart.copy(strict302Handling = true))
  def disableResponseChunksDiscarding = newResponsePart(protocol.responsePart.copy(discardResponseChunks = false))
  def extraInfoExtractor(f: ExtraInfoExtractor) = newResponsePart(protocol.responsePart.copy(extraInfoExtractor = Some(f)))
  def transformResponse(responseTransformer: PartialFunction[Response, Response]) = newResponsePart(protocol.responsePart.copy(responseTransformer = Some(responseTransformer)))
  def check(checks: HttpCheck*) = newResponsePart(protocol.responsePart.copy(checks = protocol.responsePart.checks ::: checks.toList))
  def inferHtmlResources(): HttpProtocolBuilder = inferHtmlResources(None)
  def inferHtmlResources(white: WhiteList): HttpProtocolBuilder = inferHtmlResources(Some(Filters(white, BlackList())))
  def inferHtmlResources(white: WhiteList, black: BlackList): HttpProtocolBuilder = inferHtmlResources(Some(Filters(white, black)))
  def inferHtmlResources(black: BlackList, white: WhiteList = WhiteList(Nil)): HttpProtocolBuilder = inferHtmlResources(Some(Filters(black, white)))
  private def inferHtmlResources(filters: Option[Filters]) = newResponsePart(protocol.responsePart.copy(inferHtmlResources = true, htmlResourcesInferringFilters = filters))

  // wsPart
  private def newWsPart(wsPart: HttpProtocolWsPart) = copy(protocol = copy(protocol.copy(wsPart = wsPart)))
  def wsBaseURL(url: String) = newWsPart(protocol.wsPart.copy(wsBaseURLs = List(Uri.create(url))))
  def wsBaseURLs(urls: String*): HttpProtocolBuilder = wsBaseURLs(urls.toList)
  def wsBaseURLs(urls: List[String]): HttpProtocolBuilder = newWsPart(protocol.wsPart.copy(wsBaseURLs = urls.map(Uri.create)))
  def wsReconnect = newWsPart(protocol.wsPart.copy(reconnect = true))
  def wsMaxReconnects(max: Int) = newWsPart(protocol.wsPart.copy(maxReconnects = Some(max)))

  // proxyPart
  private def newProxyPart(proxyPart: HttpProtocolProxyPart) = copy(protocol = copy(protocol.copy(proxyPart = proxyPart)))
  def noProxyFor(hosts: String*): HttpProtocolBuilder = newProxyPart(protocol.proxyPart.copy(proxyExceptions = hosts))
  def proxy(httpProxy: Proxy): HttpProtocolBuilder = newProxyPart(protocol.proxyPart.copy(proxies = Some(httpProxy.proxyServers)))

  def build = {
    require(protocol.enginePart.shareClient || !protocol.enginePart.shareConnections, "Invalid protocol configuration: if you stop sharing the HTTP client, you can't share connections!")
    protocol
  }
}
