/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine

import java.net.InetSocketAddress

import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration._
import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.util.NameGen
import io.gatling.http.HeaderNames._
import io.gatling.http.HeaderValues._
import io.gatling.http.client.{ HttpClient, HttpListener, Request, RequestBuilder }
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.Http
import io.gatling.http.client.resolver.InetAddressNameResolverWrapper
import io.gatling.http.client.uri.Uri
import io.gatling.http.client.util.{ Pair => JavaPair }
import io.gatling.http.util.{ SslContexts, SslContextsFactory }

import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.{ EventLoop, EventLoopGroup }
import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaders, HttpMethod, HttpResponseStatus }
import io.netty.handler.ssl.SslContext
import io.netty.resolver.dns._
import javax.net.ssl.KeyManagerFactory

object HttpEngine {
  def apply(coreComponents: CoreComponents): HttpEngine = {
    val sslContextsFactory = new SslContextsFactory(coreComponents.configuration.http)
    val httpClient = HttpClientFactory(coreComponents, sslContextsFactory).newClient
    new HttpEngine(sslContextsFactory, httpClient, coreComponents.eventLoopGroup, coreComponents.configuration)
  }
}

class HttpEngine(
    sslContextsFactory: SslContextsFactory,
    httpClient: HttpClient,
    eventLoopGroup: EventLoopGroup,
    configuration: GatlingConfiguration
) extends AutoCloseable
    with NameGen
    with StrictLogging {

  private[this] var warmedUp = false

  def warmUp(httpComponents: HttpComponents): Unit =
    if (!warmedUp) {
      logger.info("Start warm up")
      warmedUp = true

      import httpComponents._

      httpProtocol.warmUpUrl match {
        case Some(url) =>
          val requestBuilder = new RequestBuilder(HttpMethod.GET, Uri.create(url))
            .setHeaders(
              new DefaultHttpHeaders()
                .add(Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .add(AcceptLanguage, "en-US,en;q=0.5")
                .add(AcceptEncoding, "gzip")
                .add(Connection, Close)
                .add(UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
            )
            .setRequestTimeout(1000)
            .setDefaultCharset(configuration.core.charset)

          httpProtocol.proxyPart.proxy.foreach(requestBuilder.setProxyServer)
          val eventLoop = eventLoopGroup.next()

          try {
            val p = Promise[Unit]
            httpClient.sendRequest(
              requestBuilder.build,
              0,
              eventLoop,
              new HttpListener {
                override def onHttpResponse(httpResponseStatus: HttpResponseStatus, httpHeaders: HttpHeaders): Unit = {}

                override def onThrowable(throwable: Throwable): Unit = p.failure(throwable)

                override def onHttpResponseBodyChunk(byteBuf: ByteBuf, last: Boolean): Unit =
                  if (last) {
                    p.success(())
                  }
              },
              null,
              null
            )
            Await.result(p.future, 2 seconds)
            logger.debug(s"Warm up request $url successful")
          } catch {
            case NonFatal(e) =>
              if (logger.underlying.isDebugEnabled)
                logger.debug(s"Couldn't execute warm up request $url", e)
              else
                logger.info(s"Couldn't execute warm up request $url: ${e.rootMessage}")
          } finally {
            httpClient.flushClientIdChannels(0, eventLoop)
          }

        case _ =>
          val expression = "foo".expressionSuccess

          Http(expression)
            .get(expression)
            .header("bar", expression)
            .queryParam(expression, expression)
            .build(httpComponents.httpCaches, httpComponents.httpProtocol, throttled = false, configuration)

          Http(expression)
            .post(expression)
            .header("bar", expression)
            .formParam(expression, expression)
            .build(httpComponents.httpCaches, httpComponents.httpProtocol, throttled = false, configuration)
      }

      logger.info("Warm up done")
    }

  def executeRequest(
      clientRequest: Request,
      clientId: Long,
      shared: Boolean,
      eventLoop: EventLoop,
      listener: HttpListener,
      sslContext: SslContext,
      alpnSslContext: SslContext
  ): Unit =
    if (!httpClient.isClosed) {
      httpClient.sendRequest(clientRequest, if (shared) -1 else clientId, eventLoop, listener, sslContext, alpnSslContext)
    }

  def executeHttp2Requests(
      requestsAndListeners: Iterable[JavaPair[Request, HttpListener]],
      clientId: Long,
      shared: Boolean,
      eventLoop: EventLoop,
      sslContext: SslContext,
      alpnSslContext: SslContext
  ): Unit =
    if (!httpClient.isClosed) {
      httpClient.sendHttp2Requests(requestsAndListeners.toArray, if (shared) -1 else clientId, eventLoop, sslContext, alpnSslContext)
    }

  def newAsyncDnsNameResolver(eventLoop: EventLoop, dnsServers: Array[InetSocketAddress]): InetAddressNameResolverWrapper =
    new InetAddressNameResolverWrapper(
      new DnsNameResolverBuilder(eventLoop)
        .nameServerProvider(
          if (dnsServers.length == 0) DnsServerAddressStreamProviders.platformDefault
          else new SequentialDnsServerAddressStreamProvider(dnsServers: _*)
        )
        .queryTimeoutMillis(configuration.http.dns.queryTimeout.toMillis.toInt)
        .maxQueriesPerResolve(configuration.http.dns.maxQueriesPerResolve)
        .build()
    )

  def newSslContexts(http2Enabled: Boolean, perUserKeyManagerFactory: Option[KeyManagerFactory]): SslContexts =
    sslContextsFactory.newSslContexts(http2Enabled, perUserKeyManagerFactory)

  def flushClientIdChannels(clientId: Long, eventLoop: EventLoop): Unit =
    if (!httpClient.isClosed) {
      httpClient.flushClientIdChannels(clientId, eventLoop)
    }

  override def close(): Unit =
    httpClient.close()
}
