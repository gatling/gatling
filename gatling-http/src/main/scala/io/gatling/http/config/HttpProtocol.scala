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
package io.gatling.http.config

import java.net.InetAddress
import java.util.regex.Pattern

import akka.actor.ActorSystem
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.result.writer.DataWriters

import scala.util.control.NonFatal

import com.ning.http.client._
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider
import com.ning.http.client.providers.netty.channel.pool.ChannelPoolPartitionSelector
import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.config.{ GatlingConfiguration, Protocol }
import io.gatling.core.filter.Filters
import io.gatling.core.session.{ Session, Expression, ExpressionWrapper }
import io.gatling.core.util.RoundRobin
import io.gatling.http.HeaderNames._
import io.gatling.http.ahc.{ ChannelPoolPartitioning, HttpEngine }
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.ExtraInfoExtractor
import io.gatling.http.request.builder.Http
import io.gatling.http.response.Response

class DefaultHttpProtocol(implicit configuration: GatlingConfiguration, httpEngine: HttpEngine) {

  val value = HttpProtocol(
    baseURLs = Nil,
    warmUpUrl = configuration.http.warmUpUrl,
    httpEngine,
    enginePart = HttpProtocolEnginePart(
      shareClient = true,
      shareConnections = false,
      shareDnsCache = false,
      maxConnectionsPerHost = 6,
      virtualHost = None,
      localAddress = None),
    requestPart = HttpProtocolRequestPart(
      headers = Map.empty,
      realm = None,
      autoReferer = true,
      cache = true,
      disableUrlEncoding = false,
      silentResources = false,
      silentURI = None,
      signatureCalculator = None),
    responsePart = HttpProtocolResponsePart(
      followRedirect = true,
      maxRedirects = None,
      strict302Handling = false,
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
}

object HttpProtocol extends StrictLogging {

  def baseUrlIterator(urls: List[String]): Iterator[Option[String]] =
    urls match {
      case Nil        => Iterator.continually(None)
      case url :: Nil => Iterator.continually(Some(url))
      case _          => RoundRobin(urls.map(Some(_)).toVector)
    }

  def apply(
    baseURLs: List[String],
    warmUpUrl: Option[String],
    httpEngine: HttpEngine,
    enginePart: HttpProtocolEnginePart,
    requestPart: HttpProtocolRequestPart,
    responsePart: HttpProtocolResponsePart,
    wsPart: HttpProtocolWsPart,
    proxyPart: HttpProtocolProxyPart)(implicit configuration: GatlingConfiguration): HttpProtocol = {

    val warmUpF = (system: ActorSystem, dataWriters: DataWriters, throttler: Throttler, httProtocol: HttpProtocol) => {
      logger.info("Start warm up")

      httpEngine.start(system, dataWriters, throttler)

      warmUpUrl match {
        case Some(url) =>
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
            httpEngine.defaultAhc
              .getOrElse(throw new IllegalStateException("Trying to warmUp the HttpProtocol while the HttpEngine hasn't been started"))
              .executeRequest(requestBuilder.build).get
          } catch {
            case NonFatal(e) => logger.info(s"Couldn't execute warm up request $url", e)
          }

        case _ =>
          val expression = "foo".expression

          implicit val protocol = this
          implicit val httpCaches = httpEngine.httpCaches

          new Http(expression)
            .get(expression)
            .header("bar", expression)
            .queryParam(expression, expression)
            .build(httProtocol, throttled = false)

          new Http(expression)
            .post(expression)
            .header("bar", expression)
            .formParam(expression, expression)
            .build(httProtocol, throttled = false)
      }

      logger.info("Warm up done")
    }

    val userEndF: HttpProtocol => Session => Unit = { protocol =>

      val doNothing: Session => Unit = _ => ()

      if (protocol.enginePart.shareConnections) {
        doNothing

      } else {
        session =>
          {
            val (_, ahc) = httpEngine.httpClient(session, protocol)
            ahc.foreach(_.getProvider.asInstanceOf[NettyAsyncHttpProvider].flushChannelPoolPartitions(new ChannelPoolPartitionSelector() {

              val userBase = ChannelPoolPartitioning.partitionIdUserBase(session)

              override def select(partitionId: String): Boolean = partitionId.startsWith(userBase)
            }))
          }
      }
    }

    new HttpProtocol(baseURLs,
      warmUpUrl,
      enginePart,
      requestPart,
      responsePart,
      wsPart,
      proxyPart,
      warmUpF,
      userEndF)
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
  proxyPart: HttpProtocolProxyPart,
  warmUpF: (ActorSystem, DataWriters, Throttler, HttpProtocol) => Unit,
  userEndF: HttpProtocol => Session => Unit)
    extends Protocol {

  import HttpProtocol._

  private val httpBaseUrlIterator = baseUrlIterator(baseURLs)
  def baseURL: Option[String] = httpBaseUrlIterator.next()

  override def warmUp(system: ActorSystem, dataWriters: DataWriters, throttler: Throttler): Unit = warmUpF(system, dataWriters, throttler, this)

  override def userEnd(session: Session): Unit = userEndF(this)(session)
}

case class HttpProtocolEnginePart(
  shareClient: Boolean,
  shareConnections: Boolean,
  maxConnectionsPerHost: Int,
  shareDnsCache: Boolean,
  virtualHost: Option[Expression[String]],
  localAddress: Option[InetAddress])

case class HttpProtocolRequestPart(
  headers: Map[String, Expression[String]],
  realm: Option[Expression[Realm]],
  autoReferer: Boolean,
  cache: Boolean,
  disableUrlEncoding: Boolean,
  silentURI: Option[Pattern],
  silentResources: Boolean,
  signatureCalculator: Option[Expression[SignatureCalculator]])

case class HttpProtocolResponsePart(
  followRedirect: Boolean,
  maxRedirects: Option[Int],
  strict302Handling: Boolean,
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

  private val wsBaseUrlIterator = baseUrlIterator(wsBaseURLs)
  def wsBaseURL: Option[String] = wsBaseUrlIterator.next()
}

case class HttpProtocolProxyPart(
  proxies: Option[(ProxyServer, ProxyServer)],
  proxyExceptions: Seq[String])
