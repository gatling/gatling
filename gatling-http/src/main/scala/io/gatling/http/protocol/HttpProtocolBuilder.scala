/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import java.net.{ InetAddress, InetSocketAddress }

import io.gatling.commons.util.Platform
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.{ BlackList, Filters, WhiteList }
import io.gatling.core.session._
import io.gatling.core.session.el.El
import io.gatling.http.HeaderNames._
import io.gatling.http.ResponseTransformer
import io.gatling.http.check.HttpCheck
import io.gatling.http.client.SignatureCalculator
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.client.realm.Realm
import io.gatling.http.fetch.InferredResourceNaming
import io.gatling.http.request.builder.RequestBuilder
import io.gatling.http.util.HttpHelper

import com.softwaremill.quicklens._
import io.netty.handler.ssl.OpenSsl

/**
 * HttpProtocolBuilder class companion
 */
object HttpProtocolBuilder {

  implicit def toHttpProtocol(builder: HttpProtocolBuilder): HttpProtocol = builder.build

  def apply(configuration: GatlingConfiguration): HttpProtocolBuilder =
    HttpProtocolBuilder(HttpProtocol(configuration), configuration.http.advanced.useOpenSsl)

  val MissingEnabledHttp2ForPriorKnowledgeException = new IllegalArgumentException("Cannot set HTTP/2 prior knowledge if HTTP/2 is not enabled")
}

/**
 * Builder for HttpProtocol used in DSL
 *
 * @param protocol the protocol being built
 */
case class HttpProtocolBuilder(protocol: HttpProtocol, useOpenSsl: Boolean) {

  def baseUrl(url: String) = baseUrls(List(url))
  def baseUrls(urls: String*): HttpProtocolBuilder = baseUrls(urls.toList)
  def baseUrls(urls: List[String]): HttpProtocolBuilder = this.modify(_.protocol.baseUrls).setTo(urls)
  def warmUp(url: String): HttpProtocolBuilder = this.modify(_.protocol.warmUpUrl).setTo(Some(url))
  def disableWarmUp: HttpProtocolBuilder = this.modify(_.protocol.warmUpUrl).setTo(None)

  // enginePart
  def shareConnections = this.modify(_.protocol.enginePart.shareConnections).setTo(true)
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
  def upgradeInsecureRequestsHeader(value: Expression[String]) = header(UpgradeInsecureRequests, value)
  def basicAuth(username: Expression[String], password: Expression[String]) = authRealm(HttpHelper.buildBasicAuthRealm(username, password))
  def digestAuth(username: Expression[String], password: Expression[String]) = authRealm(HttpHelper.buildDigestAuthRealm(username, password))
  def authRealm(realm: Expression[Realm]) = this.modify(_.protocol.requestPart.realm).setTo(Some(realm))
  def silentResources = this.modify(_.protocol.requestPart.silentResources).setTo(true)
  def silentUri(regex: String) = this.modify(_.protocol.requestPart.silentUri).setTo(Some(regex.r.pattern))
  def disableUrlEncoding = this.modify(_.protocol.requestPart.disableUrlEncoding).setTo(true)
  def sign(calculator: Expression[SignatureCalculator]): HttpProtocolBuilder = this.modify(_.protocol.requestPart.signatureCalculator).setTo(Some(calculator))
  def signWithOAuth1(consumerKey: Expression[String], clientSharedSecret: Expression[String], token: Expression[String], tokenSecret: Expression[String]): HttpProtocolBuilder =
    sign(RequestBuilder.oauth1SignatureCalculator(consumerKey, clientSharedSecret, token, tokenSecret))
  def enableHttp2 =
    if ((useOpenSsl && OpenSsl.isAlpnSupported) || Platform.JavaMajorVersion >= 11) {
      this.modify(_.protocol.enginePart.enableHttp2).setTo(true)
    } else {
      throw new UnsupportedOperationException("You can't use HTTP/2 if OpenSSL is not available and Java version < 11")
    }

  def http2PriorKnowledge(remotes: Map[String, Boolean]) =
    this.modify(_.protocol.enginePart.http2PriorKnowledge).setTo(remotes.map {
      case (address, isHttp2) =>
        val remote = address.split(':') match {
          case Array(hostname, port) => Remote(hostname, port.toInt)
          case Array(hostname)       => Remote(hostname, 443)
          case _                     => throw new IllegalArgumentException("Invalid address for HTTP/2 prior knowledge: " + address)
        }
        remote -> isHttp2
    })

  // responsePart
  def disableFollowRedirect = this.modify(_.protocol.responsePart.followRedirect).setTo(false)
  def maxRedirects(max: Int) = this.modify(_.protocol.responsePart.maxRedirects).setTo(max)
  def strict302Handling = this.modify(_.protocol.responsePart.strict302Handling).setTo(true)
  def transformResponse(responseTransformer: ResponseTransformer) = this.modify(_.protocol.responsePart.responseTransformer).setTo(Some(responseTransformer))
  def check(checks: HttpCheck*) = this.modify(_.protocol.responsePart.checks).using(_ ::: checks.toList)
  def inferHtmlResources(): HttpProtocolBuilder = inferHtmlResources(None)
  def inferHtmlResources(white: WhiteList): HttpProtocolBuilder = inferHtmlResources(Some(Filters(white, BlackList())))
  def inferHtmlResources(white: WhiteList, black: BlackList): HttpProtocolBuilder = inferHtmlResources(Some(Filters(white, black)))
  def inferHtmlResources(black: BlackList, white: WhiteList = WhiteList(Nil)): HttpProtocolBuilder = inferHtmlResources(Some(Filters(black, white)))
  private def inferHtmlResources(filters: Option[Filters]) =
    this
      .modify(_.protocol.responsePart.inferHtmlResources).setTo(true)
      .modify(_.protocol.responsePart.htmlResourcesInferringFilters).setTo(filters)
  def nameInferredHtmlResourcesAfterUrlTail = nameInferredHtmlResources(InferredResourceNaming.UrlTailInferredResourceNaming)
  def nameInferredHtmlResourcesAfterAbsoluteUrl = nameInferredHtmlResources(InferredResourceNaming.AbsoluteUrlInferredResourceNaming)
  def nameInferredHtmlResourcesAfterRelativeUrl = nameInferredHtmlResources(InferredResourceNaming.RelativeUrlInferredResourceNaming)
  def nameInferredHtmlResourcesAfterPath = nameInferredHtmlResources(InferredResourceNaming.PathInferredResourceNaming)
  def nameInferredHtmlResourcesAfterLastPathElement = nameInferredHtmlResources(InferredResourceNaming.LastPathElementInferredResourceNaming)
  def nameInferredHtmlResources(f: Uri => String) = this.modify(_.protocol.responsePart.inferredHtmlResourcesNaming).setTo(f)

  // wsPart
  def wsBaseUrl(url: String) = wsBaseUrls(List(url))
  def wsBaseUrls(urls: String*): HttpProtocolBuilder = wsBaseUrls(urls.toList)
  def wsBaseUrls(urls: List[String]): HttpProtocolBuilder = this.modify(_.protocol.wsPart.wsBaseUrls).setTo(urls)
  def wsReconnect = this.modify(_.protocol.wsPart.reconnect).setTo(true)
  def wsMaxReconnects(max: Int) = this.modify(_.protocol.wsPart.maxReconnects).setTo(Some(max))

  // proxyPart
  def noProxyFor(hosts: String*): HttpProtocolBuilder = this.modify(_.protocol.proxyPart.proxyExceptions).setTo(hosts)
  def proxy(httpProxy: Proxy): HttpProtocolBuilder = this.modify(_.protocol.proxyPart.proxy).setTo(Some(httpProxy.proxyServer))

  // dnsPart
  def asyncNameResolution(dnsServers: String*): HttpProtocolBuilder =
    asyncNameResolution(dnsServers.map { dnsServer =>
      dnsServer.split(':') match {
        case Array(hostname, port) => new InetSocketAddress(hostname, port.toInt)
        case Array(hostname)       => new InetSocketAddress(hostname, 53)
        case _                     => throw new IllegalArgumentException("Invalid dnsServer: " + dnsServer)
      }
    }.toArray)
  def asyncNameResolution(dnsServers: Array[InetSocketAddress]): HttpProtocolBuilder =
    this.modify(_.protocol.dnsPart.dnsNameResolution).setTo(AsyncDnsNameResolution(dnsServers))
  def hostNameAliases(aliases: Map[String, String]): HttpProtocolBuilder = {
    val aliasesToInetAddresses = aliases.map { case (hostname, ip) => hostname -> InetAddress.getByAddress(hostname, InetAddress.getByName(ip).getAddress) }
    this.modify(_.protocol.dnsPart.hostNameAliases).setTo(aliasesToInetAddresses)
  }
  def perUserNameResolution: HttpProtocolBuilder =
    this.modify(_.protocol.dnsPart.perUserNameResolution).setTo(true)

  def build = protocol
}
