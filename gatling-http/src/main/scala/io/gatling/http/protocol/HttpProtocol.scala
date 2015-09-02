/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
import java.util.regex.Pattern

import io.gatling.commons.util.RoundRobin
import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.Filters
import io.gatling.core.protocol.{ ProtocolKey, Protocol }
import io.gatling.core.session._
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.cache.HttpCaches
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.ExtraInfoExtractor
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient._
import org.asynchttpclient.proxy._
import org.asynchttpclient.uri.Uri

object HttpProtocol extends StrictLogging {

  val HttpProtocolKey = new ProtocolKey {

    type Protocol = HttpProtocol
    type Components = HttpComponents
    def protocolClass: Class[io.gatling.core.protocol.Protocol] = classOf[HttpProtocol].asInstanceOf[Class[io.gatling.core.protocol.Protocol]]

    def defaultValue(implicit configuration: GatlingConfiguration): HttpProtocol = HttpProtocol(configuration)

    def newComponents(system: ActorSystem, coreComponents: CoreComponents)(implicit configuration: GatlingConfiguration): HttpProtocol => HttpComponents = {

      val httpEngine = HttpEngine(system, coreComponents)

      httpProtocol => {
        val httpComponents = HttpComponents(httpProtocol, httpEngine, new HttpCaches)
        httpEngine.warmpUp(httpComponents)
        httpComponents
      }
    }
  }

  def apply(configuration: GatlingConfiguration): HttpProtocol =
    HttpProtocol(
      baseURLs = Nil,
      warmUpUrl = configuration.http.warmUpUrl,
      enginePart = HttpProtocolEnginePart(
        shareClient = true,
        shareConnections = false,
        shareDnsCache = false,
        maxConnectionsPerHost = 6,
        virtualHost = None,
        localAddress = None
      ),
      requestPart = HttpProtocolRequestPart(
        headers = Map.empty,
        realm = None,
        autoReferer = true,
        cache = true,
        disableUrlEncoding = false,
        silentResources = false,
        silentURI = None,
        signatureCalculator = None
      ),
      responsePart = HttpProtocolResponsePart(
        followRedirect = true,
        maxRedirects = None,
        strict302Handling = false,
        discardResponseChunks = true,
        responseTransformer = None,
        checks = Nil,
        extraInfoExtractor = None,
        inferHtmlResources = false,
        htmlResourcesInferringFilters = None
      ),
      wsPart = HttpProtocolWsPart(
        wsBaseURLs = Nil,
        reconnect = false,
        maxReconnects = None
      ),
      proxyPart = HttpProtocolProxyPart(
        proxies = None,
        proxyExceptions = Nil
      )
    )

  def baseUrlIterator(urls: List[String]): Iterator[Option[String]] =
    urls match {
      case Nil        => Iterator.continually(None)
      case url :: Nil => Iterator.continually(Some(url))
      case _          => RoundRobin(urls.map(Some(_)).toVector)
    }
}

/**
 * Class containing the configuration for the HTTP protocol
 *
 * @param baseURLs the radixes of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param warmUpUrl the url used to load the TCP stack
 * @param enginePart the HTTP engine related configuration
 * @param requestPart the request related configuration
 * @param responsePart the response related configuration
 * @param wsPart the WebSocket related configuration
 * @param proxyPart the Proxy related configuration
 */
case class HttpProtocol(
  baseURLs:     List[String],
  warmUpUrl:    Option[String],
  enginePart:   HttpProtocolEnginePart,
  requestPart:  HttpProtocolRequestPart,
  responsePart: HttpProtocolResponsePart,
  wsPart:       HttpProtocolWsPart,
  proxyPart:    HttpProtocolProxyPart
)
    extends Protocol {

  type Components = HttpComponents

  private val httpBaseUrlIterator = HttpProtocol.baseUrlIterator(baseURLs)
  def baseURL: Option[String] = httpBaseUrlIterator.next()

  def makeAbsoluteHttpUri(url: String): Validation[Uri] =
    if (HttpHelper.isAbsoluteHttpUrl(url))
      Uri.create(url).success
    else
      baseURL match {
        case Some(root) => Uri.create(root + url).success
        case _          => s"No protocol.baseURL defined but provided url is relative : $url".failure
      }
}

case class HttpProtocolEnginePart(
  shareClient:           Boolean,
  shareConnections:      Boolean,
  maxConnectionsPerHost: Int,
  shareDnsCache:         Boolean,
  virtualHost:           Option[Expression[String]],
  localAddress:          Option[Expression[InetAddress]]
)

case class HttpProtocolRequestPart(
  headers:             Map[String, Expression[String]],
  realm:               Option[Expression[Realm]],
  autoReferer:         Boolean,
  cache:               Boolean,
  disableUrlEncoding:  Boolean,
  silentURI:           Option[Pattern],
  silentResources:     Boolean,
  signatureCalculator: Option[Expression[SignatureCalculator]]
)

case class HttpProtocolResponsePart(
  followRedirect:                Boolean,
  maxRedirects:                  Option[Int],
  strict302Handling:             Boolean,
  discardResponseChunks:         Boolean,
  responseTransformer:           Option[PartialFunction[Response, Response]],
  checks:                        List[HttpCheck],
  extraInfoExtractor:            Option[ExtraInfoExtractor],
  inferHtmlResources:            Boolean,
  htmlResourcesInferringFilters: Option[Filters]
)

case class HttpProtocolWsPart(
    wsBaseURLs:    List[String],
    reconnect:     Boolean,
    maxReconnects: Option[Int]
) {

  private val wsBaseUrlIterator = HttpProtocol.baseUrlIterator(wsBaseURLs)
  def wsBaseURL: Option[String] = wsBaseUrlIterator.next()
  def makeAbsoluteWsUri(url: String): Validation[Uri] =
    if (HttpHelper.isAbsoluteHttpUrl(url))
      Uri.create(url).success
    else
      wsBaseURL match {
        case Some(root) => Uri.create(root + url).success
        case _          => s"No protocol.wsBaseURL defined but provided url is relative : $url".failure
      }
}

case class HttpProtocolProxyPart(
  proxies:         Option[(ProxyServer, ProxyServer)],
  proxyExceptions: Seq[String]
)
