/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import java.{ util => ju }
import java.net.{ InetAddress, InetSocketAddress }
import javax.net.ssl.KeyManagerFactory

import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration._
import scala.util.control.NonFatal

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Throwables._
import io.gatling.core.CoreComponents
import io.gatling.core.body.StringBody
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.client.{ HttpClient, HttpListener, Request, RequestBuilder }
import io.gatling.http.client.resolver._
import io.gatling.http.client.uri.Uri
import io.gatling.http.client.util.Pair
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.Http
import io.gatling.http.resolver._
import io.gatling.http.util.{ SslContexts, SslContextsFactory }
import io.gatling.netty.util.Transports

import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.{ EventLoop, EventLoopGroup }
import io.netty.handler.codec.http._
import io.netty.resolver.dns._
import io.netty.util.concurrent.{ Promise => NettyPromise }

object HttpEngine {
  def apply(coreComponents: CoreComponents): HttpEngine = {
    val sslContextsFactory = new SslContextsFactory(coreComponents.configuration.ssl)
    val httpClient = new HttpClientFactory(sslContextsFactory, coreComponents.configuration).newClient
    new HttpEngine(sslContextsFactory, httpClient, coreComponents.eventLoopGroup, coreComponents.clock, coreComponents.configuration)
  }
}

final class HttpEngine(
    sslContextsFactory: SslContextsFactory,
    httpClient: HttpClient,
    eventLoopGroup: EventLoopGroup,
    clock: Clock,
    configuration: GatlingConfiguration
) extends AutoCloseable
    with StrictLogging {
  private[this] var warmedUp = false

  def warmUp(httpComponents: HttpComponents): Unit =
    if (!warmedUp) {
      logger.debug("Start warm up")
      warmedUp = true

      import httpComponents._

      // classloading
      val expression = "foo".expressionSuccess

      new Http(expression)
        .get(expression)
        .header("bar", expression)
        .queryParam(expression, expression)
        .build(httpComponents.httpCaches, httpComponents.httpProtocol, throttled = false, configuration)

      new Http(expression)
        .post(expression)
        .body(StringBody(expression, configuration.core.charset))
        .build(httpComponents.httpCaches, httpComponents.httpProtocol, throttled = false, configuration)

      val url = httpProtocol.warmUpUrl.getOrElse("https://gatling.io")
      val requestBuilder = new RequestBuilder("warmUp", HttpMethod.GET, Uri.create(url), InetAddressNameResolver.JAVA_RESOLVER)
        .setHeaders(
          new DefaultHttpHeaders()
            .add(HttpHeaderNames.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .add(HttpHeaderNames.ACCEPT_LANGUAGE, "en-US,en;q=0.5")
            .add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)
            .add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
            .add(HttpHeaderNames.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/119.0")
        )
        .setRequestTimeout(1000)
        .setDefaultCharset(configuration.core.charset)

      httpProtocol.proxyPart.proxy.foreach(requestBuilder.setProxyServer)
      val eventLoop = eventLoopGroup.next()
      // load ciphers
      val sslContexts = sslContextsFactory.newSslContexts(http2Enabled = true, None)

      try {
        if (httpProtocol.warmUpUrl.isDefined) {
          val p = Promise[Unit]()
          httpClient.sendRequest(
            requestBuilder.build,
            0,
            eventLoop,
            new HttpListener {
              override def onFinalClientRequest(request: Request): Unit = {}

              override def onHttpResponse(httpResponseStatus: HttpResponseStatus, httpHeaders: HttpHeaders): Unit = {}

              override def onThrowable(throwable: Throwable): Unit = p.failure(throwable)

              override def onHttpResponseBodyChunk(byteBuf: ByteBuf, last: Boolean): Unit =
                if (last) {
                  p.success(())
                }
            },
            sslContexts
          )
          Await.result(p.future, 2.seconds)
          logger.debug(s"Warm up request $url successful")
        }
      } catch {
        case NonFatal(e) =>
          if (logger.underlying.isDebugEnabled)
            logger.debug(s"Couldn't execute warm up request $url", e)
          else
            logger.debug(s"Couldn't execute warm up request $url: ${e.rootMessage}")
      } finally {
        httpClient.flushClientIdChannels(0, eventLoop)
        sslContexts.close()
      }

      logger.debug("Warm up done")
    }

  def executeRequest(
      clientRequest: Request,
      clientId: Long,
      shared: Boolean,
      eventLoop: EventLoop,
      listener: HttpListener,
      userSslContexts: Option[SslContexts]
  ): Unit =
    if (!httpClient.isClosed) {
      httpClient.sendRequest(clientRequest, if (shared) -1 else clientId, eventLoop, listener, userSslContexts.orNull)
    }

  def executeHttp2Requests(
      requestsAndListeners: Iterable[Pair[Request, HttpListener]],
      clientId: Long,
      shared: Boolean,
      eventLoop: EventLoop,
      userSslContexts: Option[SslContexts]
  ): Unit =
    if (!httpClient.isClosed) {
      httpClient.sendHttp2Requests(requestsAndListeners.toArray, if (shared) -1 else clientId, eventLoop, userSslContexts.orNull)
    }

  // [e]
  //
  // [e]

  def newJavaDnsNameResolver: InetAddressNameResolver =
    // [e]
    //
    //
    //
    // [e]
    InetAddressNameResolver.JAVA_RESOLVER

  def newAsyncDnsNameResolver(eventLoop: EventLoop, dnsServers: Array[InetSocketAddress], cache: DnsCache): InetAddressNameResolver =
    // [e]
    //
    //
    //
    //
    // [e]
    new InetAddressNameResolverWrapper(
      new DnsNameResolverBuilder(eventLoop)
        .datagramChannelFactory(Transports.newDatagramChannelFactory(configuration.netty.useNativeTransport, configuration.netty.useIoUring))
        .nameServerProvider(
          if (dnsServers.isEmpty) DnsServerAddressStreamProviders.platformDefault
          else new SequentialDnsServerAddressStreamProvider(dnsServers: _*)
        )
        .queryTimeoutMillis(configuration.http.dns.queryTimeout.toMillis.toInt)
        .maxQueriesPerResolve(configuration.http.dns.maxQueriesPerResolve)
        .resolveCache(cache)
        .build()
    )

  // create shared name resolvers for all the users with this protocol
  private val sharedResolverCache = new ju.concurrent.ConcurrentHashMap[EventLoop, InetAddressNameResolver]

  def newSharedAsyncDnsNameResolverFactory(dnsServers: Array[InetSocketAddress]): EventLoop => InetAddressNameResolver = {
    val inProgressResolutions = new ju.concurrent.ConcurrentHashMap[String, NettyPromise[ju.List[InetAddress]]]

    val sharedCache: DnsCache = new DefaultDnsCache

    val computer: ju.function.Function[EventLoop, InetAddressNameResolver] =
      el => {
        val actualResolver = newAsyncDnsNameResolver(el, dnsServers, sharedCache)
        new InflightInetAddressNameResolver(actualResolver, inProgressResolutions)
      }

    eventLoop => sharedResolverCache.computeIfAbsent(eventLoop, computer)
  }

  def newSslContexts(http2Enabled: Boolean, perUserKeyManagerFactory: Option[KeyManagerFactory]): SslContexts =
    sslContextsFactory.newSslContexts(http2Enabled, perUserKeyManagerFactory)

  def flushClientIdChannels(clientId: Long, eventLoop: EventLoop): Unit =
    if (!httpClient.isClosed) {
      httpClient.flushClientIdChannels(clientId, eventLoop)
    }

  override def close(): Unit = {
    httpClient.close()
    // perform close on system shutdown instead of virtual user termination as it's shared
    sharedResolverCache.values().forEach(_.close())
  }
}
