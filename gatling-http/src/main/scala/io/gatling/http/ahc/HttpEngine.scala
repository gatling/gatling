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
package io.gatling.http.ahc

import java.util.concurrent.{ Executors, ThreadFactory }

import org.jboss.netty.channel.socket.nio.{ NioWorkerPool, NioClientBossPool, NioClientSocketChannelFactory }
import org.jboss.netty.logging.{ InternalLoggerFactory, Slf4JLoggerFactory }

import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, Request }
import com.ning.http.client.providers.netty.{ NettyAsyncHttpProviderConfig, NettyConnectionsPool }
import com.ning.http.client.websocket.WebSocketUpgradeHandler
import com.typesafe.scalalogging.slf4j.StrictLogging

import akka.actor.ActorRef
import io.gatling.core.ConfigKeys
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.controller.{ Controller, ThrottledRequest }
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.action.ws.{ OnFailedOpen, WsListener }
import io.gatling.http.check.HttpCheck
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.{ ExtraInfoExtractor, HttpRequest }
import io.gatling.http.response.ResponseBuilderFactory
import io.gatling.http.util.SSLHelper.{ RichAsyncHttpClientConfigBuilder, newKeyManagers, newTrustManagers }
import io.gatling.http.check.ws.WsCheck
import io.gatling.core.check.CheckResult

case class HttpTx(session: Session,
                  request: Request,
                  requestName: String,
                  checks: List[HttpCheck],
                  responseBuilderFactory: ResponseBuilderFactory,
                  protocol: HttpProtocol,
                  next: ActorRef,
                  followRedirect: Boolean,
                  maxRedirects: Option[Int],
                  throttled: Boolean,
                  silent: Boolean,
                  explicitResources: Seq[HttpRequest],
                  extraInfoExtractor: Option[ExtraInfoExtractor],
                  resourceFetching: Boolean = false,
                  redirectCount: Int = 0)

case class WsTx(session: Session,
                request: Request,
                requestName: String,
                protocol: HttpProtocol,
                next: ActorRef,
                start: Long,
                reconnectCount: Int = 0,
                check: Option[WsCheck] = None,
                pendingCheckSuccesses: List[CheckResult] = Nil,
                updates: List[Session => Session] = Nil) {

  def applyUpdates(session: Session) = {
    val newSession = session.update(updates)
    copy(session = newSession, updates = Nil)
  }
}

object HttpEngine extends AkkaDefaults with StrictLogging {

  private var _instance: Option[HttpEngine] = None

  def start(): Unit = {
    if (!_instance.isDefined) {
      val client = new HttpEngine
      _instance = Some(client)
      system.registerOnTermination(stop())
    }
  }

  def stop(): Unit = {
    _instance.map { engine =>
      engine.applicationThreadPool.shutdown()
      engine.nioThreadPool.shutdown()
    }
    _instance = None
  }

  def instance: HttpEngine = _instance match {
    case Some(engine) => engine
    case _            => throw new UnsupportedOperationException("HTTP engine hasn't been started")
  }
}

class HttpEngine extends AkkaDefaults with StrictLogging {

  val applicationThreadPool = Executors.newCachedThreadPool(new ThreadFactory {
    override def newThread(r: Runnable) = {
      val t = new Thread(r, "Netty Thread")
      t.setDaemon(true)
      t
    }
  })

  val nioThreadPool = Executors.newCachedThreadPool

  val nettyTimer = new AkkaNettyTimer

  // set up Netty LoggerFactory for slf4j instead of default JDK
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  val connectionsPool = new NettyConnectionsPool(configuration.http.ahc.maximumConnectionsTotal,
    configuration.http.ahc.maximumConnectionsPerHost,
    configuration.http.ahc.idleConnectionInPoolTimeOutInMs,
    configuration.http.ahc.maxConnectionLifeTimeInMs,
    configuration.http.ahc.allowSslConnectionPool,
    nettyTimer)

  val nettyConfig = {
    val numWorkers = configuration.http.ahc.ioThreadMultiplier * Runtime.getRuntime.availableProcessors
    val socketChannelFactory = new NioClientSocketChannelFactory(new NioClientBossPool(nioThreadPool, 1, nettyTimer, null), new NioWorkerPool(nioThreadPool, numWorkers))
    system.registerOnTermination(socketChannelFactory.releaseExternalResources())
    val nettyConfig = new NettyAsyncHttpProviderConfig
    nettyConfig.addProperty(NettyAsyncHttpProviderConfig.SOCKET_CHANNEL_FACTORY, socketChannelFactory)
    nettyConfig.setNettyTimer(nettyTimer)
    nettyConfig
  }

  val defaultAhcConfig = {
    val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
      .setAllowPoolingConnection(configuration.http.ahc.allowPoolingConnection)
      .setAllowSslConnectionPool(configuration.http.ahc.allowSslConnectionPool)
      .setCompressionEnabled(configuration.http.ahc.compressionEnabled)
      .setConnectionTimeoutInMs(configuration.http.ahc.connectionTimeOut)
      .setIdleConnectionInPoolTimeoutInMs(configuration.http.ahc.idleConnectionInPoolTimeOutInMs)
      .setIdleConnectionTimeoutInMs(configuration.http.ahc.idleConnectionTimeOutInMs)
      .setIOThreadMultiplier(configuration.http.ahc.ioThreadMultiplier)
      .setMaximumConnectionsPerHost(configuration.http.ahc.maximumConnectionsPerHost)
      .setMaximumConnectionsTotal(configuration.http.ahc.maximumConnectionsTotal)
      .setMaxRequestRetry(configuration.http.ahc.maxRetry)
      .setRequestTimeoutInMs(configuration.http.ahc.requestTimeOutInMs)
      .setUseProxyProperties(configuration.http.ahc.useProxyProperties)
      .setUserAgent("Gatling/2.0")
      .setUseRawUrl(configuration.http.ahc.useRawUrl)
      .setExecutorService(applicationThreadPool)
      .setAsyncHttpClientProviderConfig(nettyConfig)
      .setConnectionsPool(connectionsPool)
      .setWebSocketIdleTimeoutInMs(configuration.http.ahc.webSocketIdleTimeoutInMs)
      .setUseRelativeURIsWithSSLProxies(configuration.http.ahc.useRelativeURIsWithSSLProxies)
      .setTimeConverter(JodaTimeConverter)

    val trustManagers = configuration.http.ssl.trustStore
      .map(config => newTrustManagers(config.storeType, config.file, config.password, config.algorithm))

    val keyManagers = configuration.http.ssl.keyStore
      .map(config => newKeyManagers(config.storeType, config.file, config.password, config.algorithm))

    if (trustManagers.isDefined || keyManagers.isDefined)
      ahcConfigBuilder.setSSLContext(trustManagers, keyManagers)

    ahcConfigBuilder.build
  }

  def newAHC(session: Session): AsyncHttpClient = newAHC(Some(session))

  def newAHC(session: Option[Session]) = {
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

  lazy val DefaultAHC = newAHC(None)

  val AhcAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.ahc"

  def httpClient(session: Session, protocol: HttpProtocol): (Session, AsyncHttpClient) = {
    if (protocol.enginePart.shareClient)
      (session, DefaultAHC)
    else
      session(AhcAttributeName).asOption[AsyncHttpClient] match {
        case Some(client) => (session, client)
        case _ =>
          val httpClient = newAHC(session)
          (session.set(AhcAttributeName, httpClient), httpClient)
      }
  }

  def startHttpTransaction(tx: HttpTx): Unit = {

    val (newTx, client) = {
      val (newSession, client) = httpClient(tx.session, tx.protocol)
      (tx.copy(session = newSession), client)
    }

    if (tx.throttled)
      Controller ! ThrottledRequest(tx.session.scenarioName, () => client.executeRequest(newTx.request, new AsyncHandler(newTx)))
    else
      client.executeRequest(newTx.request, new AsyncHandler(newTx))
  }

  def startWebSocketTransaction(tx: WsTx, wsActor: ActorRef): Unit = {
    val (newTx, client) = {
      val (newSession, client) = httpClient(tx.session, tx.protocol)
      (tx.copy(session = newSession), client)
    }

    try {
      val listener = new WsListener(newTx, wsActor)

      val handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build
      client.executeRequest(tx.request, handler)

    } catch {
      case e: Exception =>
        wsActor ! OnFailedOpen(newTx, e.getMessage, nowMillis)
    }
  }
}
