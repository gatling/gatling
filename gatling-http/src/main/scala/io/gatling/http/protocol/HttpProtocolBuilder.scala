/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import com.softwaremill.quicklens._
import org.asynchttpclient.{ RequestBuilderBase, Realm, Request, SignatureCalculator }

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

  def baseURL(url: String) = baseURLs(List(url))
  def baseURLs(urls: String*): HttpProtocolBuilder = baseURLs(urls.toList)
  def baseURLs(urls: List[String]): HttpProtocolBuilder = this.modify(_.protocol.baseUrls).setTo(urls)
  def warmUp(url: String): HttpProtocolBuilder = this.modify(_.protocol.warmUpUrl).setTo(Some(url))
  def disableWarmUp: HttpProtocolBuilder = this.modify(_.protocol.warmUpUrl).setTo(None)

  // enginePart
  def disableClientSharing = this.modify(_.protocol.enginePart.shareClient).setTo(false)
  def shareConnections = this.modify(_.protocol.enginePart.shareConnections).setTo(true)
  def perUserNameResolution = this.modify(_.protocol.enginePart.perUserNameResolution).setTo(true)
  def hostNameAliases(aliases: Map[String, String]) = {
    val aliasesToInetAddresses = aliases.map { case (hostname, ip) => hostname -> InetAddress.getByAddress(hostname, InetAddress.getByName(ip).getAddress) }
    this.modify(_.protocol.enginePart.hostNameAliases).setTo(aliasesToInetAddresses)
  }
  def virtualHost(virtualHost: Expression[String]) = this.modify(_.protocol.enginePart.virtualHost).setTo(Some(virtualHost))
  def localAddress(address: String) = localAddresses(List(address))
  def localAddresses(addresses: String*): HttpProtocolBuilder = localAddresses(addresses.toList)
  def localAddresses(addresses: List[String]): HttpProtocolBuilder = this.modify(_.protocol.enginePart.localAddresses).setTo(addresses.map(InetAddress.getByName))
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
  def maxConnectionsPerHost(max: Int): HttpProtocolBuilder = this.modify(_.protocol.enginePart.maxConnectionsPerHost).setTo(max)

  // requestPart
  def disableAutoReferer = this.modify(_.protocol.requestPart.autoReferer).setTo(false)
  def disableCaching = this.modify(_.protocol.requestPart.cache).setTo(false)
  def header(name: String, value: Expression[String]) = this.modify(_.protocol.requestPart.headers).using(_ + (name -> value))
  def headers(headers: Map[String, String]) = this.modify(_.protocol.requestPart.headers).using(_ ++ headers.mapValues(_.el[String]))
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
  def authRealm(realm: Expression[Realm]) = this.modify(_.protocol.requestPart.realm).setTo(Some(realm))
  def silentResources = this.modify(_.protocol.requestPart.silentResources).setTo(true)
  def silentURI(regex: String) = this.modify(_.protocol.requestPart.silentURI).setTo(Some(regex.r.pattern))
  def disableUrlEncoding = this.modify(_.protocol.requestPart.disableUrlEncoding).setTo(true)
  def signatureCalculator(calculator: Expression[SignatureCalculator]): HttpProtocolBuilder = this.modify(_.protocol.requestPart.signatureCalculator).setTo(Some(calculator))
  def signatureCalculator(calculator: SignatureCalculator): HttpProtocolBuilder = signatureCalculator(calculator.expressionSuccess)
  def signatureCalculator(calculator: (Request, RequestBuilderBase[_]) => Unit): HttpProtocolBuilder = signatureCalculator(new SignatureCalculator {
    def calculateAndAddSignature(request: Request, requestBuilder: RequestBuilderBase[_]): Unit = calculator(request, requestBuilder)
  })

  // responsePart
  def disableFollowRedirect = this.modify(_.protocol.responsePart.followRedirect).setTo(false)
  def maxRedirects(max: Int) = this.modify(_.protocol.responsePart.maxRedirects).setTo(Some(max))
  def strict302Handling = this.modify(_.protocol.responsePart.strict302Handling).setTo(true)
  def disableResponseChunksDiscarding = this.modify(_.protocol.responsePart.discardResponseChunks).setTo(false)
  def extraInfoExtractor(f: ExtraInfoExtractor) = this.modify(_.protocol.responsePart.extraInfoExtractor).setTo(Some(f))
  def transformResponse(responseTransformer: PartialFunction[Response, Response]) = this.modify(_.protocol.responsePart.responseTransformer).setTo(Some(responseTransformer))
  def check(checks: HttpCheck*) = this.modify(_.protocol.responsePart.checks).using(_ ::: checks.toList)
  def inferHtmlResources(): HttpProtocolBuilder = inferHtmlResources(None)
  def inferHtmlResources(white: WhiteList): HttpProtocolBuilder = inferHtmlResources(Some(Filters(white, BlackList())))
  def inferHtmlResources(white: WhiteList, black: BlackList): HttpProtocolBuilder = inferHtmlResources(Some(Filters(white, black)))
  def inferHtmlResources(black: BlackList, white: WhiteList = WhiteList(Nil)): HttpProtocolBuilder = inferHtmlResources(Some(Filters(black, white)))
  private def inferHtmlResources(filters: Option[Filters]) =
    this
      .modify(_.protocol.responsePart.inferHtmlResources).setTo(true)
      .modify(_.protocol.responsePart.htmlResourcesInferringFilters).setTo(filters)

  // wsPart
  def wsBaseURL(url: String) = wsBaseURLs(List(url))
  def wsBaseURLs(urls: String*): HttpProtocolBuilder = wsBaseURLs(urls.toList)
  def wsBaseURLs(urls: List[String]): HttpProtocolBuilder = this.modify(_.protocol.wsPart.wsBaseUrls).setTo(urls)
  def wsReconnect = this.modify(_.protocol.wsPart.reconnect).setTo(true)
  def wsMaxReconnects(max: Int) = this.modify(_.protocol.wsPart.maxReconnects).setTo(Some(max))

  // proxyPart
  def noProxyFor(hosts: String*): HttpProtocolBuilder = this.modify(_.protocol.proxyPart.proxyExceptions).setTo(hosts)
  def proxy(httpProxy: Proxy): HttpProtocolBuilder = this.modify(_.protocol.proxyPart.proxy).setTo(Some(httpProxy.proxyServer))

  def build = {
    require(protocol.enginePart.shareClient || !protocol.enginePart.shareConnections, "Invalid protocol configuration: if you stop sharing the HTTP client, you can't share connections!")
    protocol
  }
}
