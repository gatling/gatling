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
import java.util.regex.Pattern

import io.gatling.commons.util.RoundRobin
import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.Filters
import io.gatling.core.protocol.{ Protocol, ProtocolKey }
import io.gatling.core.session._
import io.gatling.http.ahc.{ HttpEngine, ResponseProcessor }
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

    def defaultProtocolValue(configuration: GatlingConfiguration): HttpProtocol = HttpProtocol(configuration)

    def newComponents(system: ActorSystem, coreComponents: CoreComponents): HttpProtocol => HttpComponents = {

      val httpEngine = HttpEngine(system, coreComponents)

      httpProtocol => {
        val httpComponents = HttpComponents(
          httpProtocol,
          httpEngine,
          new HttpCaches(coreComponents.configuration),
          new ResponseProcessor(coreComponents.statsEngine, httpEngine, coreComponents.configuration)(system)
        )

        httpEngine.warmpUp(httpComponents)
        httpComponents
      }
    }
  }

  def apply(configuration: GatlingConfiguration): HttpProtocol =
    HttpProtocol(
      baseUrls = Nil,
      warmUpUrl = configuration.http.warmUpUrl,
      enginePart = HttpProtocolEnginePart(
        shareClient = true,
        shareConnections = false,
        perUserNameResolution = false,
        hostNameAliases = Map.empty,
        maxConnectionsPerHost = 6,
        virtualHost = None,
        localAddresses = Nil
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
        wsBaseUrls = Nil,
        reconnect = false,
        maxReconnects = None
      ),
      proxyPart = HttpProtocolProxyPart(
        proxy = None,
        proxyExceptions = Nil
      )
    )
}

/**
 * Class containing the configuration for the HTTP protocol
 *
 * @param baseUrls the radixes of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param warmUpUrl the url used to load the TCP stack
 * @param enginePart the HTTP engine related configuration
 * @param requestPart the request related configuration
 * @param responsePart the response related configuration
 * @param wsPart the WebSocket related configuration
 * @param proxyPart the Proxy related configuration
 */
case class HttpProtocol(
    baseUrls:     List[String],
    warmUpUrl:    Option[String],
    enginePart:   HttpProtocolEnginePart,
    requestPart:  HttpProtocolRequestPart,
    responsePart: HttpProtocolResponsePart,
    wsPart:       HttpProtocolWsPart,
    proxyPart:    HttpProtocolProxyPart
) extends Protocol {

  type Components = HttpComponents

  val baseUrlIterator: Option[Iterator[String]] = baseUrls match {
    case Nil => None
    case _   => Some(RoundRobin(baseUrls.toVector))
  }

  private val doMakeAbsoluteHttpUri: String => Validation[Uri] =
    baseUrls match {
      case Nil => url => s"No protocol.baseUrl defined but provided url is relative : $url".failure
      case baseUrl :: Nil => url => Uri.create(baseUrl + url).success
      case _ =>
        val it = baseUrlIterator.get
        url => Uri.create(it.next() + url).success
    }

  def makeAbsoluteHttpUri(url: String): Validation[Uri] =
    if (HttpHelper.isAbsoluteHttpUrl(url))
      Uri.create(url).success
    else
      doMakeAbsoluteHttpUri(url)
}

case class HttpProtocolEnginePart(
  shareClient:           Boolean,
  shareConnections:      Boolean,
  maxConnectionsPerHost: Int,
  perUserNameResolution: Boolean,
  hostNameAliases:       Map[String, InetAddress],
  virtualHost:           Option[Expression[String]],
  localAddresses:        List[InetAddress]
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
    wsBaseUrls:    List[String],
    reconnect:     Boolean,
    maxReconnects: Option[Int]
) {

  private val wsBaseUrlIterator: Option[Iterator[String]] = wsBaseUrls match {
    case Nil => None
    case _   => Some(RoundRobin(wsBaseUrls.toVector))
  }

  private val doMakeAbsoluteWsUri: String => Validation[Uri] =
    wsBaseUrls match {
      case Nil => url => s"No protocol.wsBaseUrl defined but provided url is relative : $url".failure
      case wsBaseUrl :: Nil => url => Uri.create(wsBaseUrl + url).success
      case _ =>
        val it = wsBaseUrlIterator.get
        url => Uri.create(it.next() + url).success
    }

  def makeAbsoluteWsUri(url: String): Validation[Uri] =
    if (HttpHelper.isAbsoluteWsUrl(url))
      Uri.create(url).success
    else
      doMakeAbsoluteWsUri(url)
}

case class HttpProtocolProxyPart(
  proxy:           Option[ProxyServer],
  proxyExceptions: Seq[String]
)
