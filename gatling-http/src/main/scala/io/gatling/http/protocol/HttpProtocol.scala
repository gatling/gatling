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

import java.{ util => ju }
import java.net.InetAddress
import java.util.regex.Pattern
import javax.net.ssl.KeyManagerFactory

import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.Filters
import io.gatling.core.protocol.{ Protocol, ProtocolKey }
import io.gatling.core.session._
import io.gatling.http.ResponseTransformer
import io.gatling.http.cache.HttpCaches
import io.gatling.http.check.HttpCheck
import io.gatling.http.client.SignatureCalculator
import io.gatling.http.client.proxy.ProxyServer
import io.gatling.http.client.realm.Realm
import io.gatling.http.client.uri.Uri
import io.gatling.http.engine.HttpEngine
import io.gatling.http.engine.response.DefaultStatsProcessor
import io.gatling.http.engine.tx.HttpTxExecutor
import io.gatling.http.fetch.InferredResourceNaming

import com.typesafe.scalalogging.StrictLogging

object HttpProtocol extends StrictLogging {

  val HttpProtocolKey: ProtocolKey[HttpProtocol, HttpComponents] = new ProtocolKey[HttpProtocol, HttpComponents] {

    override def protocolClass: Class[Protocol] = classOf[HttpProtocol].asInstanceOf[Class[Protocol]]

    override def defaultProtocolValue(configuration: GatlingConfiguration): HttpProtocol = HttpProtocol(configuration)

    override def newComponents(coreComponents: CoreComponents): HttpProtocol => HttpComponents = {
      val httpEngine = HttpEngine(coreComponents)
      coreComponents.actorSystem.registerOnTermination(httpEngine.close())
      val httpCaches = new HttpCaches(coreComponents)
      val defaultStatsProcessor = new DefaultStatsProcessor(coreComponents.statsEngine)

      httpProtocol => {
        val httpComponents = new HttpComponents(
          httpProtocol,
          httpEngine,
          httpCaches,
          new HttpTxExecutor(coreComponents, httpEngine, httpCaches, defaultStatsProcessor, httpProtocol)
        )

        httpEngine.warmUp(httpComponents)
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
        localIpV4Addresses = Nil,
        localIpV6Addresses = Nil,
        enableHttp2 = false,
        http2PriorKnowledge = Map.empty,
        perUserKeyManagerFactory = None
      ),
      requestPart = HttpProtocolRequestPart(
        headers = Map.empty,
        realm = None,
        autoReferer = true,
        cache = true,
        disableUrlEncoding = false,
        silentResources = false,
        silentUri = None,
        signatureCalculator = None
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
        maxReconnects = 0
      ),
      proxyPart = HttpProtocolProxyPart(
        proxy = None,
        proxyExceptions = Nil
      ),
      dnsPart = HttpProtocolDnsPart(
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
final case class HttpProtocol(
    baseUrls: List[String],
    warmUpUrl: Option[String],
    enginePart: HttpProtocolEnginePart,
    requestPart: HttpProtocolRequestPart,
    responsePart: HttpProtocolResponsePart,
    wsPart: HttpProtocolWsPart,
    proxyPart: HttpProtocolProxyPart,
    dnsPart: HttpProtocolDnsPart
) extends Protocol {

  type Components = HttpComponents
}

final case class HttpProtocolEnginePart(
    shareConnections: Boolean,
    maxConnectionsPerHost: Int,
    virtualHost: Option[Expression[String]],
    localIpV4Addresses: List[InetAddress],
    localIpV6Addresses: List[InetAddress],
    enableHttp2: Boolean,
    http2PriorKnowledge: Map[Remote, Boolean],
    perUserKeyManagerFactory: Option[Long => KeyManagerFactory]
)

final case class HttpProtocolRequestPart(
    headers: Map[CharSequence, Expression[String]],
    realm: Option[Expression[Realm]],
    autoReferer: Boolean,
    cache: Boolean,
    disableUrlEncoding: Boolean,
    silentUri: Option[Pattern],
    silentResources: Boolean,
    signatureCalculator: Option[Expression[SignatureCalculator]]
)

final case class HttpProtocolResponsePart(
    followRedirect: Boolean,
    maxRedirects: Int,
    strict302Handling: Boolean,
    responseTransformer: Option[ResponseTransformer],
    checks: List[HttpCheck],
    inferHtmlResources: Boolean,
    inferredHtmlResourcesNaming: Uri => String,
    htmlResourcesInferringFilters: Option[Filters]
)

final case class HttpProtocolWsPart(
    wsBaseUrls: List[String],
    maxReconnects: Int
)

final case class HttpProtocolProxyPart(
    proxy: Option[ProxyServer],
    proxyExceptions: Seq[String]
)

final case class HttpProtocolDnsPart(
    dnsNameResolution: DnsNameResolution,
    hostNameAliases: Map[String, ju.List[InetAddress]],
    perUserNameResolution: Boolean
)
