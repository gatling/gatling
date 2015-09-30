/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
import java.util.concurrent.TimeUnit

import io.gatling.core.{ CoreComponents, ConfigKeys }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.http.util.SslHelper._

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ EventLoopGroup, Channel }
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import io.netty.util.internal.logging.{ Slf4JLoggerFactory, InternalLoggerFactory }
import io.netty.util.{ Timer, HashedWheelTimer }
import org.asynchttpclient.AdvancedConfig.{ LazyResponseBodyPartFactory, NettyWebSocketFactory }
import org.asynchttpclient._
import org.asynchttpclient.AdvancedConfig
import org.asynchttpclient.netty.channel.pool.{ ChannelPool, DefaultChannelPool }
import org.asynchttpclient.netty.ws.NettyWebSocket
import org.asynchttpclient.ws.WebSocketListener

private[gatling] object AhcFactory {

  def apply(system: ActorSystem, coreComponents: CoreComponents)(implicit configuration: GatlingConfiguration): AhcFactory = {
    //
    //
    //
    //
    //
    //
    new DefaultAhcFactory(system, coreComponents)
  }
}

private[gatling] trait AhcFactory {

  def defaultAhc: AsyncHttpClient

  def newAhc(session: Session): AsyncHttpClient
}

private[gatling] class DefaultAhcFactory(system: ActorSystem, coreComponents: CoreComponents)(implicit val configuration: GatlingConfiguration) extends AhcFactory with StrictLogging {

  import configuration.http.{ ahc => ahcConfig }

  // set up Netty LoggerFactory for slf4j instead of default JDK
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  private def newEventLoopGroup: EventLoopGroup = {
    val eventLoopGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("gatling-netty-thread"))
    system.registerOnTermination(eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS))
    eventLoopGroup
  }

  private def newTimer: Timer = {
    val timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS)
    timer.start()
    system.registerOnTermination(timer.stop())
    timer
  }

  private def newChannelPool(timer: Timer): ChannelPool = {
    new DefaultChannelPool(
      ahcConfig.pooledConnectionIdleTimeout,
      ahcConfig.connectionTTL,
      ahcConfig.allowPoolingSslConnections,
      timer
    )
  }

  private def newAdvancedConfig(eventLoopGroup: EventLoopGroup, timer: Timer, channelPool: ChannelPool): AdvancedConfig = {

    val advancedConfig = new AdvancedConfig
    advancedConfig.setEventLoopGroup(eventLoopGroup)
    advancedConfig.setNettyTimer(timer)
    advancedConfig.setChannelPool(channelPool)
    advancedConfig.setNettyWebSocketFactory(new NettyWebSocketFactory {
      override def newNettyWebSocket(channel: Channel, config: AsyncHttpClientConfig): NettyWebSocket =
        new NettyWebSocket(channel, config, new JArrayList[WebSocketListener](1))
    })
    advancedConfig.setBodyPartFactory(new LazyResponseBodyPartFactory)
    advancedConfig
  }

  private[gatling] def newAhcConfigBuilder(advancedConfig: AdvancedConfig) = {
    val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
      .setAllowPoolingConnections(ahcConfig.allowPoolingConnections)
      .setAllowPoolingSslConnections(ahcConfig.allowPoolingSslConnections)
      .setCompressionEnforced(ahcConfig.compressionEnforced)
      .setConnectTimeout(ahcConfig.connectTimeout)
      .setPooledConnectionIdleTimeout(ahcConfig.pooledConnectionIdleTimeout)
      .setReadTimeout(ahcConfig.readTimeout)
      .setConnectionTTL(ahcConfig.connectionTTL)
      .setMaxConnectionsPerHost(ahcConfig.maxConnectionsPerHost)
      .setMaxConnections(ahcConfig.maxConnections)
      .setMaxRequestRetry(ahcConfig.maxRetry)
      .setRequestTimeout(ahcConfig.requestTimeOut)
      .setUseProxyProperties(false)
      .setUserAgent(null)
      .setAdvancedConfig(advancedConfig)
      .setWebSocketTimeout(ahcConfig.webSocketTimeout)
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
      .setHttpClientCodecMaxInitialLineLength(ahcConfig.httpClientCodecMaxInitialLineLength)
      .setHttpClientCodecMaxHeaderSize(ahcConfig.httpClientCodecMaxHeaderSize)
      .setHttpClientCodecMaxChunkSize(ahcConfig.httpClientCodecMaxChunkSize)
      .setKeepEncodingHeader(true)
      .setWebSocketMaxFrameSize(ahcConfig.webSocketMaxFrameSize)

    val trustManagers = configuration.http.ssl.trustStore
      .map(config => newTrustManagers(config.storeType, config.file, config.password, config.algorithm))

    val keyManagers = configuration.http.ssl.keyStore
      .map(config => newKeyManagers(config.storeType, config.file, config.password, config.algorithm))

    if (trustManagers.isDefined || keyManagers.isDefined)
      ahcConfigBuilder.setSSLContext(trustManagers, keyManagers)

    ahcConfigBuilder
  }

  private val defaultAhcConfig = {
    val eventLoopGroup = newEventLoopGroup
    val timer = newTimer
    val channelPool = newChannelPool(timer)
    val advancedConfig = newAdvancedConfig(eventLoopGroup, timer, channelPool)
    val ahcConfigBuilder = newAhcConfigBuilder(advancedConfig)
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

    val client = new DefaultAsyncHttpClient(ahcConfig)
    system.registerOnTermination(client.close())
    client
  }
}
