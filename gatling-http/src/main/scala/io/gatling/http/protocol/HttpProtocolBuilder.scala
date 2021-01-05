/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.net.{ Inet4Address, InetAddress, InetSocketAddress }
import java.util.regex.Pattern
import javax.net.ssl.KeyManagerFactory

import scala.jdk.CollectionConverters._

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.{ BlackList, Filters, WhiteList }
import io.gatling.core.session._
import io.gatling.core.session.el.El
import io.gatling.http.{ MissingNettyHttpHeaderNames, ResponseTransformer }
import io.gatling.http.check.HttpCheck
import io.gatling.http.client.SignatureCalculator
import io.gatling.http.client.realm.Realm
import io.gatling.http.client.uri.Uri
import io.gatling.http.fetch.InferredResourceNaming
import io.gatling.http.request.builder.RequestBuilder
import io.gatling.http.util.{ HttpHelper, InetAddresses }

import com.softwaremill.quicklens._
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.ssl.SslProvider

object HttpProtocolBuilder {

  implicit def toHttpProtocol(builder: HttpProtocolBuilder): HttpProtocol = builder.build

  def apply(configuration: GatlingConfiguration): HttpProtocolBuilder =
    HttpProtocolBuilder(HttpProtocol(configuration), configuration.ssl.useOpenSsl)
}

final case class HttpProtocolBuilder(protocol: HttpProtocol, useOpenSsl: Boolean) {

  def baseUrl(url: String): HttpProtocolBuilder = baseUrls(List(url))
  def baseUrls(urls: String*): HttpProtocolBuilder = baseUrls(urls.toList)
  def baseUrls(urls: List[String]): HttpProtocolBuilder = this.modify(_.protocol.baseUrls).setTo(urls)
  def warmUp(url: String): HttpProtocolBuilder = this.modify(_.protocol.warmUpUrl).setTo(Some(url))
  def disableWarmUp: HttpProtocolBuilder = this.modify(_.protocol.warmUpUrl).setTo(None)

  // enginePart
  def shareConnections: HttpProtocolBuilder = disableAutoReferer.modify(_.protocol.enginePart.shareConnections).setTo(true)
  def virtualHost(virtualHost: Expression[String]): HttpProtocolBuilder = this.modify(_.protocol.enginePart.virtualHost).setTo(Some(virtualHost))
  def localAddress(address: String): HttpProtocolBuilder = localAddresses(address :: Nil)
  def localAddresses(addresses: String*): HttpProtocolBuilder = localAddresses(addresses.toList)
  def localAddresses(addresses: List[String]): HttpProtocolBuilder = {
    val (ipV4Addresses, ipV6Addresses) = addresses.map(InetAddress.getByName).partition(_.isInstanceOf[Inet4Address])
    localAddresses(ipV4Addresses, ipV6Addresses)
  }
  def useAllLocalAddresses: HttpProtocolBuilder = useAllLocalAddressesMatching()
  def useAllLocalAddressesMatching(patterns: String*): HttpProtocolBuilder = {
    val compiledPatterns = patterns.map(Pattern.compile)

    def filter(addresses: List[InetAddress]): List[InetAddress] =
      addresses.filter { address =>
        val hostAddress = address.getHostAddress
        compiledPatterns.exists(_.matcher(hostAddress).matches)
      }

    localAddresses(filter(InetAddresses.AllIpV4LocalAddresses), filter(InetAddresses.AllIpV6LocalAddresses))
  }

  private def localAddresses(ipV4Addresses: List[InetAddress], ipV6Addresses: List[InetAddress]): HttpProtocolBuilder =
    this
      .modify(_.protocol.enginePart.localIpV4Addresses)
      .setTo(ipV4Addresses)
      .modify(_.protocol.enginePart.localIpV6Addresses)
      .setTo(ipV6Addresses)

  def maxConnectionsPerHostLikeFirefoxOld: HttpProtocolBuilder = maxConnectionsPerHost(2)
  def maxConnectionsPerHostLikeFirefox: HttpProtocolBuilder = maxConnectionsPerHost(6)
  def maxConnectionsPerHostLikeOperaOld: HttpProtocolBuilder = maxConnectionsPerHost(4)
  def maxConnectionsPerHostLikeOpera: HttpProtocolBuilder = maxConnectionsPerHost(6)
  def maxConnectionsPerHostLikeSafariOld: HttpProtocolBuilder = maxConnectionsPerHost(4)
  def maxConnectionsPerHostLikeSafari: HttpProtocolBuilder = maxConnectionsPerHost(6)
  def maxConnectionsPerHostLikeIE7: HttpProtocolBuilder = maxConnectionsPerHost(2)
  def maxConnectionsPerHostLikeIE8: HttpProtocolBuilder = maxConnectionsPerHost(6)
  def maxConnectionsPerHostLikeIE10: HttpProtocolBuilder = maxConnectionsPerHost(8)
  def maxConnectionsPerHostLikeChrome: HttpProtocolBuilder = maxConnectionsPerHost(6)
  def maxConnectionsPerHost(max: Int): HttpProtocolBuilder = this.modify(_.protocol.enginePart.maxConnectionsPerHost).setTo(max)
  def perUserKeyManagerFactory(f: Long => KeyManagerFactory): HttpProtocolBuilder = this.modify(_.protocol.enginePart.perUserKeyManagerFactory).setTo(Some(f))

  // requestPart
  def disableAutoReferer: HttpProtocolBuilder = this.modify(_.protocol.requestPart.autoReferer).setTo(false)
  def disableCaching: HttpProtocolBuilder = this.modify(_.protocol.requestPart.cache).setTo(false)
  def header(name: CharSequence, value: Expression[String]): HttpProtocolBuilder = this.modify(_.protocol.requestPart.headers).using(_ + (name -> value))
  def headers(headers: Map[_ <: CharSequence, String]): HttpProtocolBuilder =
    this.modify(_.protocol.requestPart.headers).using(_ ++ headers.view.mapValues(_.el[String]))
  def acceptHeader(value: Expression[String]): HttpProtocolBuilder = header(HttpHeaderNames.ACCEPT, value)
  def acceptCharsetHeader(value: Expression[String]): HttpProtocolBuilder = header(HttpHeaderNames.ACCEPT_CHARSET, value)
  def acceptEncodingHeader(value: Expression[String]): HttpProtocolBuilder = header(HttpHeaderNames.ACCEPT_ENCODING, value)
  def acceptLanguageHeader(value: Expression[String]): HttpProtocolBuilder = header(HttpHeaderNames.ACCEPT_LANGUAGE, value)
  def authorizationHeader(value: Expression[String]): HttpProtocolBuilder = header(HttpHeaderNames.AUTHORIZATION, value)
  def connectionHeader(value: Expression[String]): HttpProtocolBuilder = header(HttpHeaderNames.CONNECTION, value)
  def contentTypeHeader(value: Expression[String]): HttpProtocolBuilder = header(HttpHeaderNames.CONTENT_TYPE, value)
  def doNotTrackHeader(value: Expression[String]): HttpProtocolBuilder = header(MissingNettyHttpHeaderNames.DNT, value)
  def userAgentHeader(value: Expression[String]): HttpProtocolBuilder = header(HttpHeaderNames.USER_AGENT, value)
  def upgradeInsecureRequestsHeader(value: Expression[String]): HttpProtocolBuilder = header(MissingNettyHttpHeaderNames.UpgradeInsecureRequests, value)
  def basicAuth(username: Expression[String], password: Expression[String]): HttpProtocolBuilder = authRealm(HttpHelper.buildBasicAuthRealm(username, password))
  def digestAuth(username: Expression[String], password: Expression[String]): HttpProtocolBuilder =
    authRealm(HttpHelper.buildDigestAuthRealm(username, password))
  def authRealm(realm: Expression[Realm]): HttpProtocolBuilder = this.modify(_.protocol.requestPart.realm).setTo(Some(realm))
  def silentResources: HttpProtocolBuilder = this.modify(_.protocol.requestPart.silentResources).setTo(true)
  def silentUri(regex: String): HttpProtocolBuilder = this.modify(_.protocol.requestPart.silentUri).setTo(Some(regex.r.pattern))
  def disableUrlEncoding: HttpProtocolBuilder = this.modify(_.protocol.requestPart.disableUrlEncoding).setTo(true)
  def sign(calculator: Expression[SignatureCalculator]): HttpProtocolBuilder = this.modify(_.protocol.requestPart.signatureCalculator).setTo(Some(calculator))
  def signWithOAuth1(
      consumerKey: Expression[String],
      clientSharedSecret: Expression[String],
      token: Expression[String],
      tokenSecret: Expression[String]
  ): HttpProtocolBuilder =
    sign(RequestBuilder.oauth1SignatureCalculator(consumerKey, clientSharedSecret, token, tokenSecret))
  def enableHttp2: HttpProtocolBuilder =
    if (SslProvider.isAlpnSupported(if (useOpenSsl) SslProvider.OPENSSL_REFCNT else SslProvider.JDK)) {
      this.modify(_.protocol.enginePart.enableHttp2).setTo(true)
    } else {
      throw new UnsupportedOperationException("You can't use HTTP/2 if OpenSSL is not available and Java version < 11")
    }

  def http2PriorKnowledge(remotes: Map[String, Boolean]): HttpProtocolBuilder =
    this
      .modify(_.protocol.enginePart.http2PriorKnowledge)
      .setTo(remotes.map { case (address, isHttp2) =>
        val remote = address.split(':') match {
          case Array(hostname, port) => new Remote(hostname, port.toInt)
          case Array(hostname)       => new Remote(hostname, 443)
          case _                     => throw new IllegalArgumentException("Invalid address for HTTP/2 prior knowledge: " + address)
        }
        remote -> isHttp2
      })

  // responsePart
  def disableFollowRedirect: HttpProtocolBuilder = this.modify(_.protocol.responsePart.followRedirect).setTo(false)
  def maxRedirects(max: Int): HttpProtocolBuilder = this.modify(_.protocol.responsePart.maxRedirects).setTo(max)
  def strict302Handling: HttpProtocolBuilder = this.modify(_.protocol.responsePart.strict302Handling).setTo(true)
  def transformResponse(responseTransformer: ResponseTransformer): HttpProtocolBuilder =
    this.modify(_.protocol.responsePart.responseTransformer).setTo(Some(responseTransformer))
  def check(checks: HttpCheck*): HttpProtocolBuilder = this.modify(_.protocol.responsePart.checks).using(_ ::: checks.toList)
  def inferHtmlResources(): HttpProtocolBuilder = inferHtmlResources(None)
  def inferHtmlResources(white: WhiteList): HttpProtocolBuilder = inferHtmlResources(Some(new Filters(white, BlackList.Empty)))
  def inferHtmlResources(white: WhiteList, black: BlackList): HttpProtocolBuilder = inferHtmlResources(Some(new Filters(white, black)))
  def inferHtmlResources(black: BlackList): HttpProtocolBuilder = inferHtmlResources(Some(new Filters(black, WhiteList.Empty)))
  def inferHtmlResources(black: BlackList, white: WhiteList): HttpProtocolBuilder = inferHtmlResources(Some(new Filters(black, white)))
  private def inferHtmlResources(filters: Option[Filters]): HttpProtocolBuilder =
    this
      .modify(_.protocol.responsePart.inferHtmlResources)
      .setTo(true)
      .modify(_.protocol.responsePart.htmlResourcesInferringFilters)
      .setTo(filters)
  def nameInferredHtmlResourcesAfterUrlTail: HttpProtocolBuilder = nameInferredHtmlResources(InferredResourceNaming.UrlTailInferredResourceNaming)
  def nameInferredHtmlResourcesAfterAbsoluteUrl: HttpProtocolBuilder = nameInferredHtmlResources(InferredResourceNaming.AbsoluteUrlInferredResourceNaming)
  def nameInferredHtmlResourcesAfterRelativeUrl: HttpProtocolBuilder = nameInferredHtmlResources(InferredResourceNaming.RelativeUrlInferredResourceNaming)
  def nameInferredHtmlResourcesAfterPath: HttpProtocolBuilder = nameInferredHtmlResources(InferredResourceNaming.PathInferredResourceNaming)
  def nameInferredHtmlResourcesAfterLastPathElement: HttpProtocolBuilder =
    nameInferredHtmlResources(InferredResourceNaming.LastPathElementInferredResourceNaming)
  def nameInferredHtmlResources(f: Uri => String): HttpProtocolBuilder = this.modify(_.protocol.responsePart.inferredHtmlResourcesNaming).setTo(f)

  // wsPart
  def wsBaseUrl(url: String): HttpProtocolBuilder = wsBaseUrls(List(url))
  def wsBaseUrls(urls: String*): HttpProtocolBuilder = wsBaseUrls(urls.toList)
  def wsBaseUrls(urls: List[String]): HttpProtocolBuilder = this.modify(_.protocol.wsPart.wsBaseUrls).setTo(urls)
  def wsReconnect: HttpProtocolBuilder = wsMaxReconnects(Int.MaxValue)
  def wsMaxReconnects(max: Int): HttpProtocolBuilder = this.modify(_.protocol.wsPart.maxReconnects).setTo(max)

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
  def hostNameAliases(aliases: Map[String, List[String]]): HttpProtocolBuilder = {
    val aliasesToInetAddresses = aliases.map { case (hostname, ips) =>
      hostname -> ips.map(ip => InetAddress.getByAddress(hostname, InetAddress.getByName(ip).getAddress)).asJava
    }
    this.modify(_.protocol.dnsPart.hostNameAliases).setTo(aliasesToInetAddresses)
  }
  def perUserNameResolution: HttpProtocolBuilder =
    this.modify(_.protocol.dnsPart.perUserNameResolution).setTo(true)

  def build: HttpProtocol = protocol
}
