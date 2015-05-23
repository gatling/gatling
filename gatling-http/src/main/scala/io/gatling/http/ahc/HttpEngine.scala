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
package io.gatling.http.ahc

import java.util.{ ArrayList => JArrayList }
import java.util.concurrent.{ TimeUnit, Executors, ThreadFactory }

import scala.util.control.NonFatal

import io.gatling.http.request.builder.Http

import akka.actor.{ ActorSystem, ActorRef }
import akka.routing.RoundRobinPool

import io.gatling.core.{ CoreComponents, ConfigKeys }
import io.gatling.core.akka.ActorNames
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.HeaderNames._
import io.gatling.http.HeaderValues._
import io.gatling.http.fetch.ResourceFetcher
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol }
import io.gatling.http.util.SslHelper._
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.socket.nio.{ NioWorkerPool, NioClientBossPool, NioClientSocketChannelFactory }
import org.jboss.netty.logging.{ InternalLoggerFactory, Slf4JLoggerFactory }
import org.jboss.netty.util.HashedWheelTimer
import com.ning.http.client.{ RequestBuilder, AsyncHttpClient, AsyncHttpClientConfig }
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig.NettyWebSocketFactory
import com.ning.http.client.providers.netty.ws.NettyWebSocket
import com.ning.http.client.providers.netty.channel.pool.DefaultChannelPool
import com.ning.http.client.ws.WebSocketListener
import com.typesafe.scalalogging.StrictLogging

object HttpEngine {
  val AhcAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.ahc"
}

class HttpEngine(system: ActorSystem, val coreComponents: CoreComponents)(implicit val configuration: GatlingConfiguration) extends ResourceFetcher with ActorNames with StrictLogging {

  // set up Netty LoggerFactory for slf4j instead of default JDK
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  private val defaultAhcConfig = {
    import configuration.http.{ ahc => ahcConfig }

    val applicationThreadPool = Executors.newCachedThreadPool(new ThreadFactory {
      override def newThread(r: Runnable) = {
        val t = new Thread(r, "Netty Thread")
        t.setDaemon(true)
        t
      }
    })

    val nioThreadPool = Executors.newCachedThreadPool

    system.registerOnTermination(() => {
      applicationThreadPool.shutdown()
      nioThreadPool.shutdown()
    })

    val nettyTimer = {
      val timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS)
      timer.start()
      system.registerOnTermination(timer.stop())
      timer
    }

    val channelPool = new DefaultChannelPool(ahcConfig.pooledConnectionIdleTimeout,
      ahcConfig.connectionTTL,
      ahcConfig.allowPoolingSslConnections,
      nettyTimer)

    val nettyConfig = {
      val numWorkers = ahcConfig.ioThreadMultiplier * Runtime.getRuntime.availableProcessors
      val socketChannelFactory = new NioClientSocketChannelFactory(new NioClientBossPool(nioThreadPool, 1, nettyTimer, null), new NioWorkerPool(nioThreadPool, numWorkers))
      system.registerOnTermination(socketChannelFactory.releaseExternalResources())
      val nettyConfig = new NettyAsyncHttpProviderConfig
      nettyConfig.setSocketChannelFactory(socketChannelFactory)
      nettyConfig.setNettyTimer(nettyTimer)
      nettyConfig.setChannelPool(channelPool)
      nettyConfig.setHttpClientCodecMaxInitialLineLength(ahcConfig.httpClientCodecMaxInitialLineLength)
      nettyConfig.setHttpClientCodecMaxHeaderSize(ahcConfig.httpClientCodecMaxHeaderSize)
      nettyConfig.setHttpClientCodecMaxChunkSize(ahcConfig.httpClientCodecMaxChunkSize)
      nettyConfig.setNettyWebSocketFactory(new NettyWebSocketFactory {
        override def newNettyWebSocket(channel: Channel, nettyConfig: NettyAsyncHttpProviderConfig): NettyWebSocket =
          new NettyWebSocket(channel, nettyConfig, new JArrayList[WebSocketListener](1))
      })
      nettyConfig.setKeepEncodingHeader(ahcConfig.keepEncodingHeader)
      nettyConfig.setWebSocketMaxFrameSize(ahcConfig.webSocketMaxFrameSize)
      nettyConfig
    }

    val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
      .setAllowPoolingConnections(ahcConfig.allowPoolingConnections)
      .setAllowPoolingSslConnections(ahcConfig.allowPoolingSslConnections)
      .setCompressionEnforced(ahcConfig.compressionEnforced)
      .setConnectTimeout(ahcConfig.connectTimeout)
      .setPooledConnectionIdleTimeout(ahcConfig.pooledConnectionIdleTimeout)
      .setReadTimeout(ahcConfig.readTimeout)
      .setConnectionTTL(ahcConfig.connectionTTL)
      .setIOThreadMultiplier(ahcConfig.ioThreadMultiplier)
      .setMaxConnectionsPerHost(ahcConfig.maxConnectionsPerHost)
      .setMaxConnections(ahcConfig.maxConnections)
      .setMaxRequestRetry(ahcConfig.maxRetry)
      .setRequestTimeout(ahcConfig.requestTimeOut)
      .setUseProxyProperties(ahcConfig.useProxyProperties)
      .setUserAgent(null)
      .setExecutorService(applicationThreadPool)
      .setAsyncHttpClientProviderConfig(nettyConfig)
      .setWebSocketTimeout(ahcConfig.webSocketTimeout)
      .setUseRelativeURIsWithConnectProxies(ahcConfig.useRelativeURIsWithConnectProxies)
      .setAcceptAnyCertificate(ahcConfig.acceptAnyCertificate)
      .setEnabledProtocols(ahcConfig.sslEnabledProtocols match {
        case Nil => null
        case ps  => ps.toArray
      })
      .setEnabledCipherSuites(ahcConfig.sslEnabledCipherSuites match {
        case Nil => null
        case ps  => ps.toArray
      })
      .setSslSessionCacheSize(if (ahcConfig.sslSessionCacheSize > 0) ahcConfig.sslSessionCacheSize else null)
      .setSslSessionTimeout(if (ahcConfig.sslSessionTimeout > 0) ahcConfig.sslSessionTimeout else null)

    val trustManagers = configuration.http.ssl.trustStore
      .map(config => newTrustManagers(config.storeType, config.file, config.password, config.algorithm))

    val keyManagers = configuration.http.ssl.keyStore
      .map(config => newKeyManagers(config.storeType, config.file, config.password, config.algorithm))

    if (trustManagers.isDefined || keyManagers.isDefined)
      ahcConfigBuilder.setSSLContext(trustManagers, keyManagers)

    ahcConfigBuilder.build
  }

  val asyncHandlerActors: ActorRef = {
    val poolSize = 3 * Runtime.getRuntime.availableProcessors
    val asyncHandlerActors = system.actorOf(RoundRobinPool(poolSize).props(AsyncHandlerActor.props(coreComponents.statsEngine, this)), actorName("asyncHandler"))

    asyncHandlerActors
  }

  private val defaultAhc = newAhc(None)

  private def newAhc(session: Session): AsyncHttpClient = newAhc(Some(session))

  private def newAhc(session: Option[Session]) = {
    val ahcConfig = session.flatMap { session =>

      val trustManagers = for {
        file <- session(ConfigKeys.http.ssl.trustStore.File).asOption[String]
        password <- session(ConfigKeys.http.ssl.trustStore.Password).asOption[String]
        storeType = session(ConfigKeys.http.ssl.trustStore.Type).asOption[String]
        algorithm = session(ConfigKeys.http.ssl.trustStore.Algorithm).asOption[String]
      } yield newTrustManagers(storeType, file, password, algorithm)

      val keyManagers = for {
        file <- session(ConfigKeys.http.ssl.keyStore.File).asOption[String]
        password <- session(ConfigKeys.http.ssl.keyStore.Password).asOption[String]
        storeType = session(ConfigKeys.http.ssl.keyStore.Type).asOption[String]
        algorithm = session(ConfigKeys.http.ssl.keyStore.Algorithm).asOption[String]
      } yield newKeyManagers(storeType, file, password, algorithm)

      trustManagers.orElse(keyManagers).map { _ =>
        logger.info(s"Setting a custom SSLContext for user ${session.userId}")
        new AsyncHttpClientConfig.Builder(defaultAhcConfig).setSSLContext(trustManagers, keyManagers).build
      }

    }.getOrElse(defaultAhcConfig)

    val client = new AsyncHttpClient(ahcConfig)
    system.registerOnTermination(client.close())
    client
  }

  def httpClient(session: Session, httpProtocol: HttpProtocol): (Session, AsyncHttpClient) = {
    if (httpProtocol.enginePart.shareClient)
      (session, defaultAhc)
    else
      session(HttpEngine.AhcAttributeName).asOption[AsyncHttpClient] match {
        case Some(client) => (session, client)
        case _ =>
          val httpClient = newAhc(session)
          (session.set(HttpEngine.AhcAttributeName, httpClient), httpClient)
      }
  }

  private var warmedUp = false

  def warmpUp(httpComponents: HttpComponents): Unit =
    if (!warmedUp) {
      logger.info("Start warm up")
      warmedUp = true

      import httpComponents._

      httpProtocol.warmUpUrl match {
        case Some(url) =>
          val requestBuilder = new RequestBuilder().setUrl(url)
            .setHeader(Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .setHeader(AcceptLanguage, "en-US,en;q=0.5")
            .setHeader(AcceptEncoding, "gzip")
            .setHeader(Connection, KeepAlive)
            .setHeader(UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
            .setRequestTimeout(100)

          httpProtocol.proxyPart.proxies.foreach {
            case (httpProxy, httpsProxy) =>
              val proxy = if (url.startsWith("https")) httpsProxy else httpProxy
              requestBuilder.setProxyServer(proxy)
          }

          try {
            defaultAhc.executeRequest(requestBuilder.build).get
          } catch {
            case NonFatal(e) => logger.info(s"Couldn't execute warm up request $url", e)
          }

        case _ =>
          val expression = "foo".expression

          implicit val protocol = this

          new Http(expression)
            .get(expression)
            .header("bar", expression)
            .queryParam(expression, expression)
            .build(httpComponents, throttled = false)

          new Http(expression)
            .post(expression)
            .header("bar", expression)
            .formParam(expression, expression)
            .build(httpComponents, throttled = false)
      }

      logger.info("Warm up done")
    }
}
