/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import java.util.concurrent.TimeUnit

import io.gatling.core.{ CoreComponents, ConfigKeys }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.http.util.SslHelper._

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.resolver.dns.ExtendedDnsNameResolver
import io.netty.util.concurrent.DefaultThreadFactory
import io.netty.util.internal.logging.{ Slf4JLoggerFactory, InternalLoggerFactory }
import io.netty.util.{ Timer, HashedWheelTimer }
import org.asynchttpclient.AsyncHttpClientConfig._
import org.asynchttpclient._
import org.asynchttpclient.channel.ChannelPool
import org.asynchttpclient.netty.channel.DefaultChannelPool

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

  def newNameResolver(): ExtendedDnsNameResolver
}

private[gatling] class DefaultAhcFactory(system: ActorSystem, coreComponents: CoreComponents)(implicit val configuration: GatlingConfiguration) extends AhcFactory with StrictLogging {

  import configuration.http.{ ahc => ahcConfig }

  // set up Netty LoggerFactory for slf4j instead of default JDK
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  private[this] def newEventLoopGroup(poolName: String): EventLoopGroup = {
    val eventLoopGroup = new NioEventLoopGroup(0, new DefaultThreadFactory(poolName))
    system.registerOnTermination(eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS))
    eventLoopGroup
  }

  private[this] def newTimer: Timer = {
    val timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS)
    timer.start()
    system.registerOnTermination(timer.stop())
    timer
  }

  private[this] def newChannelPool(timer: Timer): ChannelPool = {
    new DefaultChannelPool(
      ahcConfig.pooledConnectionIdleTimeout,
      ahcConfig.connectionTtl,
      timer
    )
  }

  private[gatling] def newAhcConfigBuilder(eventLoopGroup: EventLoopGroup, timer: Timer, channelPool: ChannelPool) = {
    val ahcConfigBuilder = new DefaultAsyncHttpClientConfig.Builder()
      .setKeepAlive(ahcConfig.keepAlive)
      .setCompressionEnforced(ahcConfig.compressionEnforced)
      .setConnectTimeout(ahcConfig.connectTimeout)
      .setPooledConnectionIdleTimeout(ahcConfig.pooledConnectionIdleTimeout)
      .setReadTimeout(ahcConfig.readTimeout)
      .setConnectionTtl(ahcConfig.connectionTtl)
      .setMaxConnectionsPerHost(ahcConfig.maxConnectionsPerHost)
      .setMaxConnections(ahcConfig.maxConnections)
      .setMaxRequestRetry(ahcConfig.maxRetry)
      .setRequestTimeout(ahcConfig.requestTimeOut)
      .setUseProxyProperties(false)
      .setUserAgent(null)
      .setEventLoopGroup(eventLoopGroup)
      .setNettyTimer(timer)
      .setChannelPool(channelPool)
      .setResponseBodyPartFactory(ResponseBodyPartFactory.LAZY)
      .setAcceptAnyCertificate(ahcConfig.acceptAnyCertificate)
      .setEnabledProtocols(ahcConfig.sslEnabledProtocols match {
        case Nil => null
        case ps  => ps.toArray
      })
      .setEnabledCipherSuites(ahcConfig.sslEnabledCipherSuites match {
        case Nil => null
        case ps  => ps.toArray
      })
      .setSslSessionCacheSize(ahcConfig.sslSessionCacheSize)
      .setSslSessionTimeout(ahcConfig.sslSessionTimeout)
      .setHttpClientCodecMaxInitialLineLength(ahcConfig.httpClientCodecMaxInitialLineLength)
      .setHttpClientCodecMaxHeaderSize(ahcConfig.httpClientCodecMaxHeaderSize)
      .setHttpClientCodecMaxChunkSize(ahcConfig.httpClientCodecMaxChunkSize)
      .setKeepEncodingHeader(true)
      .setWebSocketMaxFrameSize(ahcConfig.webSocketMaxFrameSize)
      .setUseOpenSsl(ahcConfig.useOpenSsl)
      .setUseNativeTransport(ahcConfig.useNativeTransport)
      .setValidateResponseHeaders(false)
      .setUsePooledMemory(ahcConfig.usePooledMemory)

    val keyManagerFactory = configuration.http.ssl.keyStore
      .map(config => newKeyManagerFactory(config.storeType, config.file, config.password, config.algorithm))

    val trustManagerFactory = configuration.http.ssl.trustStore
      .map(config => newTrustManagerFactory(config.storeType, config.file, config.password, config.algorithm))

    if (keyManagerFactory.isDefined || trustManagerFactory.isDefined)
      ahcConfigBuilder.setSslContext(ahcConfig, keyManagerFactory, trustManagerFactory)

    ahcConfigBuilder
  }

  private[this] val defaultAhcConfig = {
    val eventLoopGroup = newEventLoopGroup("gatling-http-thread")
    val timer = newTimer
    val channelPool = newChannelPool(timer)
    val ahcConfigBuilder = newAhcConfigBuilder(eventLoopGroup, timer, channelPool)
    ahcConfigBuilder.build
  }

  override val defaultAhc = newAhc(None)

  override def newAhc(session: Session): AsyncHttpClient = newAhc(Some(session))

  private[this] def newAhc(session: Option[Session]) = {
    val config = session.flatMap { session =>

      val keyManagerFactory = for {
        file <- session(ConfigKeys.http.ssl.keyStore.File).asOption[String]
        password <- session(ConfigKeys.http.ssl.keyStore.Password).asOption[String]
      } yield {
        val storeType = session(ConfigKeys.http.ssl.keyStore.Type).asOption[String]
        val algorithm = session(ConfigKeys.http.ssl.keyStore.Algorithm).asOption[String]
        newKeyManagerFactory(storeType, file, password, algorithm)
      }

      val trustManagerFactory = for {
        file <- session(ConfigKeys.http.ssl.trustStore.File).asOption[String]
        password <- session(ConfigKeys.http.ssl.trustStore.Password).asOption[String]
      } yield {
        val storeType = session(ConfigKeys.http.ssl.trustStore.Type).asOption[String]
        val algorithm = session(ConfigKeys.http.ssl.trustStore.Algorithm).asOption[String]
        newTrustManagerFactory(storeType, file, password, algorithm)
      }

      trustManagerFactory.orElse(keyManagerFactory).map { _ =>
        logger.info(s"Setting a custom SslContext for user ${session.userId}")
        new DefaultAsyncHttpClientConfig.Builder(defaultAhcConfig).setSslContext(ahcConfig, keyManagerFactory, trustManagerFactory).build
      }

    }.getOrElse(defaultAhcConfig)

    val client = new DefaultAsyncHttpClient(config)
    system.registerOnTermination(client.close())
    client
  }

  def newNameResolver(): ExtendedDnsNameResolver = {
    val executor = newEventLoopGroup("gatling-dns-thread")
    val resolver = new ExtendedDnsNameResolver(executor.next(), configuration)
    system.registerOnTermination(resolver.close())
    resolver
  }
}
