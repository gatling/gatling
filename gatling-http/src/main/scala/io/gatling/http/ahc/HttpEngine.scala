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
import java.util.concurrent.{ ExecutorService, TimeUnit, Executors, ThreadFactory }

import akka.actor.{ ActorContext, ActorSystem, Props, ActorRef }
import akka.routing.RoundRobinPool

import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.socket.nio.{ NioWorkerPool, NioClientBossPool, NioClientSocketChannelFactory }
import org.jboss.netty.logging.{ InternalLoggerFactory, Slf4JLoggerFactory }
import org.jboss.netty.util.HashedWheelTimer
import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig }
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig.NettyWebSocketFactory
import com.ning.http.client.providers.netty.ws.NettyWebSocket
import com.ning.http.client.providers.netty.channel.pool.{ ChannelPool, DefaultChannelPool }
import com.ning.http.client.ws.{ WebSocketListener, WebSocketUpgradeHandler }
import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.ConfigKeys
import io.gatling.core.akka.ActorNames
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.result.message.OK
import io.gatling.core.result.writer.StatsEngine
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.TimeHelper._
import io.gatling.http.fetch.{ RegularResourceFetched, ResourceFetcher }
import io.gatling.http.action.sse.SseHandler
import io.gatling.http.action.ws.WsListener
import io.gatling.http.cache.{ ContentCacheEntry, HttpCaches }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.util.SslHelper._

object HttpEngine {
  val AhcAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.ahc"
}

class HttpEngine(implicit val configuration: GatlingConfiguration, val httpCaches: HttpCaches) extends ResourceFetcher with ActorNames with StrictLogging {

  // set up Netty LoggerFactory for slf4j instead of default JDK
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  private[this] var _state: Option[InternalState] = None

  def start(system: ActorSystem, statsEngine: StatsEngine, throttler: Throttler): Unit = {
    _state = Some(loadInternalState(system, statsEngine, throttler))
    system.registerOnTermination(stop())
  }

  private def stop(): Unit = _state.foreach { state =>
    state.applicationThreadPool.shutdown()
    state.nioThreadPool.shutdown()
    _state = None
  }

  private def startHttpTransactionWithCache(origTx: HttpTx, ctx: ActorContext)(f: HttpTx => Unit): Unit = {
    val tx = httpCaches.applyPermanentRedirect(origTx)
    val uri = tx.request.ahcRequest.getUri
    val method = tx.request.ahcRequest.getMethod

    httpCaches.contentCacheEntry(tx.session, uri, method) match {

      case None =>
        f(tx)

      case Some(ContentCacheEntry(Some(expire), _, _)) if nowMillis > expire =>
        val newTx = tx.copy(session = httpCaches.clearContentCache(tx.session, uri, method))
        f(newTx)

      case _ =>
        resourceFetcherActorForCachedPage(uri, tx) match {
          case Some(resourceFetcherActor) =>
            logger.info(s"Fetching resources of cached page request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenario}, userId=${tx.session.userId}")
            ctx.actorOf(Props(resourceFetcherActor()), actorName("resourceFetcher"))

          case None =>
            logger.info(s"Skipping cached request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenario}, userId=${tx.session.userId}")
            if (tx.root)
              tx.next ! tx.session
            else
              tx.next ! RegularResourceFetched(uri, OK, Session.Identity, tx.silent)
        }
    }
  }

  def startHttpTransaction(origTx: HttpTx)(implicit ctx: ActorContext): Unit =
    startHttpTransactionWithCache(origTx, ctx) { tx =>

      logger.info(s"Sending request=${tx.request.requestName} uri=${tx.request.ahcRequest.getUri}: scenario=${tx.session.scenario}, userId=${tx.session.userId}")

      val requestConfig = tx.request.config

      httpClient(tx.session, requestConfig.protocol) match {
        case (newSession, Some(client)) =>
          val newTx = tx.copy(session = newSession)
          val ahcRequest = newTx.request.ahcRequest
          val handler = new AsyncHandler(newTx, this)

          if (requestConfig.throttled)
            _state.foreach(_.throttler.throttle(tx.session.scenario, () => client.executeRequest(ahcRequest, handler)))
          else
            client.executeRequest(ahcRequest, handler)

        case _ => // client has been shutdown, ignore
      }
    }

  def startWsTransaction(tx: WsTx, wsActor: ActorRef): Unit = {
    val (newTx, client) = {
      val (newSession, client) = httpClient(tx.session, tx.protocol)
      (tx.copy(session = newSession), client)
    }

    val listener = new WsListener(newTx, wsActor)

    val handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build
    client.foreach(_.executeRequest(tx.request, handler))
  }

  def startSseTransaction(tx: SseTx, sseActor: ActorRef): Unit = {
    val (newTx, client) = {
      val (newSession, client) = httpClient(tx.session, tx.protocol)
      (tx.copy(session = newSession), client)
    }

    val handler = new SseHandler(newTx, sseActor)
    client.foreach(_.executeRequest(newTx.request, handler))
  }

  private def loadInternalState(system: ActorSystem, statsEngine: StatsEngine, throttler: Throttler) = {

    import configuration.http.{ ahc => ahcConfig }

    val applicationThreadPool = Executors.newCachedThreadPool(new ThreadFactory {
      override def newThread(r: Runnable) = {
        val t = new Thread(r, "Netty Thread")
        t.setDaemon(true)
        t
      }
    })

    val nioThreadPool = Executors.newCachedThreadPool

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

    val defaultAhcConfig = {
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

    val poolSize = 3 * Runtime.getRuntime.availableProcessors
    val asyncHandlerActors = system.actorOf(RoundRobinPool(poolSize).props(AsyncHandlerActor.props(this)), actorName("asyncHandler"))

    new InternalState(applicationThreadPool, nioThreadPool, channelPool, nettyConfig, defaultAhcConfig, statsEngine, throttler, asyncHandlerActors, system)
  }

  def httpClient(session: Session, protocol: HttpProtocol): (Session, Option[AsyncHttpClient]) = {
    if (protocol.enginePart.shareClient)
      (session, defaultAhc)
    else
      session(HttpEngine.AhcAttributeName).asOption[AsyncHttpClient] match {
        case client: Some[AsyncHttpClient] => (session, client)
        case _ =>
          val httpClient = _state.map(_.newAhc(session))
          (httpClient.map(client => session.set(HttpEngine.AhcAttributeName, client)).getOrElse(session), httpClient)
      }
  }

  case class InternalState(applicationThreadPool: ExecutorService,
                           nioThreadPool: ExecutorService,
                           channelPool: ChannelPool,
                           nettyConfig: NettyAsyncHttpProviderConfig,
                           defaultAhcConfig: AsyncHttpClientConfig,
                           statsEngine: StatsEngine,
                           throttler: Throttler,
                           asyncHandlerActors: ActorRef,
                           system: ActorSystem) {

    val defaultAhc = newAhc(None)

    def newAhc(session: Session): AsyncHttpClient = newAhc(Some(session))

    def newAhc(session: Option[Session]) = {
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
  }

  def defaultAhc: Option[AsyncHttpClient] = _state.map(_.defaultAhc)
  def statsEngine: Option[StatsEngine] = _state.map(_.statsEngine)
  def asyncHandlerActors: Option[ActorRef] = _state.map(_.asyncHandlerActors)
}
