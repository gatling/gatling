/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.http.ResponseTransformer
import io.gatling.http.cache.HttpCaches
import io.gatling.http.check.HttpCheck
import io.gatling.http.client.SignatureCalculator
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.client.proxy.ProxyServer
import io.gatling.http.client.realm.Realm
import io.gatling.http.engine.HttpEngine
import io.gatling.http.engine.response.DefaultStatsProcessor
import io.gatling.http.engine.tx.HttpTxExecutor
import io.gatling.http.fetch.InferredResourceNaming
import io.gatling.http.util.HttpHelper

import com.typesafe.scalalogging.StrictLogging

object HttpProtocol extends StrictLogging {

  val HttpProtocolKey: ProtocolKey[HttpProtocol, HttpComponents] = new ProtocolKey[HttpProtocol, HttpComponents] {

    override def protocolClass: Class[Protocol] = classOf[HttpProtocol].asInstanceOf[Class[Protocol]]

    override def defaultProtocolValue(configuration: GatlingConfiguration): HttpProtocol = HttpProtocol(configuration)

    override def newComponents(coreComponents: CoreComponents): HttpProtocol => HttpComponents = {

      val httpEngine = HttpEngine(coreComponents)
      val httpCaches = new HttpCaches(coreComponents)
      val defaultStatsProcessor = new DefaultStatsProcessor(coreComponents.configuration.core.charset, coreComponents.statsEngine)

      httpProtocol => {
        val httpComponents = HttpComponents(
          coreComponents,
          httpProtocol,
          httpEngine,
          httpCaches,
          new HttpTxExecutor(coreComponents, httpEngine, httpCaches, defaultStatsProcessor, httpProtocol)
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
        shareConnections = false,
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
        signatureCalculator = None,
        enableHttp2 = false,
        http2PriorKnowledge = Map.empty
      ),
      responsePart = HttpProtocolResponsePart(
        followRedirect = true,
        maxRedirects = 20,
        strict302Handling = false,
        responseTransformer = None,
        checks = Nil,
        inferHtmlResources = false,
        inferredHtmlResourcesNaming = InferredResourceNaming.UrlTailInferredResourceNaming,
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
      ),
      dnsPart = DnsPart(
        dnsNameResolution = JavaDnsNameResolution,
        hostNameAliases = Map.empty,
        perUserNameResolution = false
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
 * @param dnsPart the DNS related configuration
 */
case class HttpProtocol(
    baseUrls:     List[String],
    warmUpUrl:    Option[String],
    enginePart:   HttpProtocolEnginePart,
    requestPart:  HttpProtocolRequestPart,
    responsePart: HttpProtocolResponsePart,
    wsPart:       HttpProtocolWsPart,
    proxyPart:    HttpProtocolProxyPart,
    dnsPart:      DnsPart
) extends Protocol {

  type Components = HttpComponents
}

case class HttpProtocolEnginePart(
    shareConnections:      Boolean,
    maxConnectionsPerHost: Int,
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
    signatureCalculator: Option[Expression[SignatureCalculator]],
    enableHttp2:         Boolean,
    http2PriorKnowledge: Map[Remote, Boolean]
)

case class HttpProtocolResponsePart(
    followRedirect:                Boolean,
    maxRedirects:                  Int,
    strict302Handling:             Boolean,
    responseTransformer:           Option[ResponseTransformer],
    checks:                        List[HttpCheck],
    inferHtmlResources:            Boolean,
    inferredHtmlResourcesNaming:   Uri => String,
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

case class DnsPart(
    dnsNameResolution:     DnsNameResolution,
    hostNameAliases:       Map[String, InetAddress],
    perUserNameResolution: Boolean
)
