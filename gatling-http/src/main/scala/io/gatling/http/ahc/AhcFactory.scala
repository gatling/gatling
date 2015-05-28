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
import java.util.concurrent.{ ExecutorService, TimeUnit, ThreadFactory, Executors }

import io.gatling.core.{ CoreComponents, ConfigKeys }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.util.ReflectionHelper._
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.util.SslHelper._

import akka.actor.ActorSystem
import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig }
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig.NettyWebSocketFactory
import com.ning.http.client.providers.netty.channel.pool.{ ChannelPool, DefaultChannelPool }
import com.ning.http.client.providers.netty.ws.NettyWebSocket
import com.ning.http.client.ws.WebSocketListener
import com.typesafe.scalalogging.StrictLogging
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.socket.nio.{ NioWorkerPool, NioClientBossPool, NioClientSocketChannelFactory }
import org.jboss.netty.logging.{ Slf4JLoggerFactory, InternalLoggerFactory }
import org.jboss.netty.util.{ Timer, HashedWheelTimer }

private[gatling] object AhcFactory {

  val AhcFactorySystemProperty = "gatling.ahcFactory"

  def apply(system: ActorSystem, coreComponents: CoreComponents)(implicit configuration: GatlingConfiguration): AhcFactory =
    sys.props.get(AhcFactorySystemProperty).map(newInstance(_, system, coreComponents))
      .getOrElse(new DefaultAhcFactory(system, coreComponents))
}

private[gatling] trait AhcFactory {

  def defaultAhc: AsyncHttpClient

  def newAhc(session: Session): AsyncHttpClient
}

private[gatling] class DefaultAhcFactory(system: ActorSystem, coreComponents: CoreComponents)(implicit val configuration: GatlingConfiguration) extends AhcFactory with StrictLogging {

  import configuration.http.{ ahc => ahcConfig }

  // set up Netty LoggerFactory for slf4j instead of default JDK
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  private def newApplicationThreadPool: ExecutorService = {

    val applicationThreadPool = Executors.newCachedThreadPool(new ThreadFactory {
      override def newThread(r: Runnable) = {
        val t = new Thread(r, "Netty Thread")
        t.setDaemon(true)
        t
      }
    })
    system.registerOnTermination(() => applicationThreadPool.shutdown())
    applicationThreadPool
  }

  private def newNioThreadPool: ExecutorService = {
    val nioThreadPool = Executors.newCachedThreadPool
    system.registerOnTermination(() => nioThreadPool.shutdown())
    nioThreadPool
  }

  private def newTimer: Timer = {
    val timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS)
    timer.start()
    system.registerOnTermination(timer.stop())
    timer
  }

  private def newChannelPool(timer: Timer): ChannelPool = {
    new DefaultChannelPool(ahcConfig.pooledConnectionIdleTimeout,
      ahcConfig.connectionTTL,
      ahcConfig.allowPoolingSslConnections,
      timer)
  }

  private def newNettyConfig(nioThreadPool: ExecutorService, timer: Timer, channelPool: ChannelPool): NettyAsyncHttpProviderConfig = {
    val numWorkers = ahcConfig.ioThreadMultiplier * Runtime.getRuntime.availableProcessors
    val socketChannelFactory = new NioClientSocketChannelFactory(new NioClientBossPool(nioThreadPool, 1, timer, null), new NioWorkerPool(nioThreadPool, numWorkers))
    system.registerOnTermination(socketChannelFactory.releaseExternalResources())
    val nettyConfig = new NettyAsyncHttpProviderConfig
    nettyConfig.setSocketChannelFactory(socketChannelFactory)
    nettyConfig.setNettyTimer(timer)
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

  private def newAhcConfigBuilder(applicationThreadPool: ExecutorService, nettyConfig: NettyAsyncHttpProviderConfig) = {
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

    ahcConfigBuilder
  }

  private val defaultAhcConfig = {
    val applicationThreadPool = newApplicationThreadPool
    val nioThreadPool = newNioThreadPool
    val timer = newTimer
    val channelPool = newChannelPool(timer)
    val nettyConfig = newNettyConfig(nioThreadPool, timer, channelPool)
    val ahcConfigBuilder = newAhcConfigBuilder(applicationThreadPool, nettyConfig)
    ahcConfigBuilder.build
  }

  override val defaultAhc = newAhc(None)

  override def newAhc(session: Session): AsyncHttpClient = newAhc(Some(session))

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
}
