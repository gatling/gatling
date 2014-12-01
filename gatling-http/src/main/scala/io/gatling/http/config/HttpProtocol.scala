/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.config

import java.net.InetAddress
import java.util.regex.Pattern

import scala.collection.mutable

import com.ning.http.client._
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider
import com.ning.http.client.providers.netty.channel.pool.ChannelPoolPartitionSelector
import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.Protocol
import io.gatling.core.filter.Filters
import io.gatling.core.session.{ Session, Expression, ExpressionWrapper }
import io.gatling.core.util.RoundRobin
import io.gatling.http.HeaderNames._
import io.gatling.http.ahc.{ ChannelPoolPartitioning, AsyncHandlerActor, HttpEngine }
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.ExtraInfoExtractor
import io.gatling.http.request.builder.Http
import io.gatling.http.response.Response

/**
 * HttpProtocol class companion
 */
object HttpProtocol {

  val DefaultHttpProtocol = HttpProtocol(
    baseURLs = Nil,
    warmUpUrl = configuration.http.warmUpUrl,
    enginePart = HttpProtocolEnginePart(
      shareClient = true,
      shareConnections = false,
      maxConnectionsPerHost = 6,
      virtualHost = None,
      localAddress = None),
    requestPart = HttpProtocolRequestPart(
      headers = Map.empty,
      realm = None,
      autoReferer = true,
      cache = true,
      disableUrlEscaping = false,
      silentResources = false,
      silentURI = None,
      signatureCalculator = None),
    responsePart = HttpProtocolResponsePart(
      followRedirect = true,
      maxRedirects = None,
      discardResponseChunks = true,
      responseTransformer = None,
      checks = Nil,
      extraInfoExtractor = None,
      inferHtmlResources = false,
      htmlResourcesInferringFilters = None),
    wsPart = HttpProtocolWsPart(
      wsBaseURLs = Nil,
      reconnect = false,
      maxReconnects = None),
    proxyPart = HttpProtocolProxyPart(
      proxies = None,
      proxyExceptions = Nil))

  val WarmUpUrls = mutable.Set.empty[String]

  GatlingActorSystem.instanceOpt.foreach(_.registerOnTermination(WarmUpUrls.clear()))

  def nextBaseUrlF(urls: List[String]): () => Option[String] =
    urls match {
      case Nil => () => None
      case url :: Nil => () => Some(url)
      case _ =>
        val roundRobinUrls = RoundRobin(urls.map(Some(_)).toVector)
        () => roundRobinUrls.next()
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
    baseURLs: List[String],
    warmUpUrl: Option[String],
    enginePart: HttpProtocolEnginePart,
    requestPart: HttpProtocolRequestPart,
    responsePart: HttpProtocolResponsePart,
    wsPart: HttpProtocolWsPart,
    proxyPart: HttpProtocolProxyPart) extends Protocol with StrictLogging {

  import HttpProtocol._

  private val baseURLF = nextBaseUrlF(baseURLs)
  def baseURL: Option[String] = baseURLF()

  override def warmUp(): Unit = {

    logger.info("Start warm up")

    HttpEngine.start()
    AsyncHandlerActor.start()

    warmUpUrl.map { url =>
      if (!WarmUpUrls.contains(url)) {
        WarmUpUrls += url
        val requestBuilder = new RequestBuilder().setUrl(url)
          .setHeader(Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          .setHeader(AcceptLanguage, "en-US,en;q=0.5")
          .setHeader(AcceptEncoding, "gzip")
          .setHeader(Connection, "keep-alive")
          .setHeader(UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
          .setRequestTimeout(2000)

        proxyPart.proxies.foreach {
          case (httpProxy, httpsProxy) =>
            val proxy = if (url.startsWith("https")) httpsProxy else httpProxy
            requestBuilder.setProxyServer(proxy)
        }

        try {
          HttpEngine.instance.DefaultAHC.executeRequest(requestBuilder.build).get
        } catch {
          case e: Exception => logger.info(s"Couldn't execute warm up request $url", e)
        }
      }
    }

    if (WarmUpUrls.isEmpty) {
      val expression = "foo".expression

      new Http(expression)
        .get(expression)
        .header("bar", expression)
        .queryParam(expression, expression)
        .build(DefaultHttpProtocol, throttled = false)

      new Http(expression)
        .post(expression)
        .header("bar", expression)
        .formParam(expression, expression)
        .build(DefaultHttpProtocol, throttled = false)
    }

    logger.info("Warm up done")
  }

  override def userEnd(session: Session): Unit = {
    val (_, ahc) = HttpEngine.instance.httpClient(session, this)
    ahc.getProvider.asInstanceOf[NettyAsyncHttpProvider].flushChannelPoolPartitions(new ChannelPoolPartitionSelector() {

      val userBase = ChannelPoolPartitioning.partitionIdUserBase(session)

      override def select(partitionId: String): Boolean = partitionId.startsWith(userBase)
    })
  }
}

case class HttpProtocolEnginePart(
  shareClient: Boolean,
  shareConnections: Boolean,
  maxConnectionsPerHost: Int,
  virtualHost: Option[Expression[String]],
  localAddress: Option[InetAddress])

case class HttpProtocolRequestPart(
  headers: Map[String, Expression[String]],
  realm: Option[Expression[Realm]],
  autoReferer: Boolean,
  cache: Boolean,
  disableUrlEscaping: Boolean,
  silentURI: Option[Pattern],
  silentResources: Boolean,
  signatureCalculator: Option[Expression[SignatureCalculator]])

case class HttpProtocolResponsePart(
  followRedirect: Boolean,
  maxRedirects: Option[Int],
  discardResponseChunks: Boolean,
  responseTransformer: Option[PartialFunction[Response, Response]],
  checks: List[HttpCheck],
  extraInfoExtractor: Option[ExtraInfoExtractor],
  inferHtmlResources: Boolean,
  htmlResourcesInferringFilters: Option[Filters])

case class HttpProtocolWsPart(
    wsBaseURLs: List[String],
    reconnect: Boolean,
    maxReconnects: Option[Int]) {

  import HttpProtocol._

  private val wsBaseURLF = nextBaseUrlF(wsBaseURLs)
  def wsBaseURL: Option[String] = wsBaseURLF()
}

case class HttpProtocolProxyPart(
  proxies: Option[(ProxyServer, ProxyServer)],
  proxyExceptions: Seq[String])
