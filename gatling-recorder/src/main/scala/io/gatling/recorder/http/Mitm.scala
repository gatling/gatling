/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.http

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration._

import io.gatling.commons.model.Credentials
import io.gatling.commons.util.Clock
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.ssl.SslServerContext

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.{ Bootstrap, ServerBootstrap }
import io.netty.channel.{ Channel, ChannelInitializer, ChannelOption, EventLoopGroup }
import io.netty.channel.group.{ ChannelGroup, DefaultChannelGroup }
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{ NioServerSocketChannel, NioSocketChannel }
import io.netty.handler.codec.http._
import io.netty.util.concurrent.GlobalEventExecutor

object Mitm extends StrictLogging {

  val SslHandlerName = "ssl"
  val HttpCodecHandlerName = "http"
  val GatlingClientHandler = "gatling-client"
  val GatlingServerHandler = "gatling-server"

  def apply(controller: RecorderController, clock: Clock, config: RecorderConfiguration): Mitm = {

    import config.netty._

    val serverChannelGroup = new DefaultChannelGroup("Gatling_Recorder", GlobalEventExecutor.INSTANCE)
    val clientEventLoopGroup = new NioEventLoopGroup
    val serverBossEventLoopGroup = new NioEventLoopGroup(1)
    val serverWorkerEventLoopGroup = new NioEventLoopGroup
    val actorSystem = ActorSystem("recorder")

    val trafficLogger = new TrafficLogger(controller)
    val sslServerContext = SslServerContext(config)
    val httpClientCodecFactory = () => new HttpClientCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize)

    val outgoingProxy =
      config.proxy.outgoing.host.map { proxyHost =>
        val port = config.proxy.outgoing.port.getOrElse(80)
        val securedPort = config.proxy.outgoing.sslPort.orElse(config.proxy.outgoing.port).getOrElse(443)
        val credentials =
          for {
            username <- config.proxy.outgoing.username
            password <- config.proxy.outgoing.password
          } yield Credentials(username, password)
        OutgoingProxy(proxyHost, port, securedPort, credentials)
      }

    val clientBootstrap =
      new Bootstrap()
        .channel(classOf[NioSocketChannel])
        .group(clientEventLoopGroup)
        .option(ChannelOption.TCP_NODELAY, java.lang.Boolean.TRUE)
        .handler(new ChannelInitializer[Channel] {
          override def initChannel(ch: Channel): Unit = {
            logger.debug("Open new client channel")
            ch.pipeline
              .addLast(HttpCodecHandlerName, httpClientCodecFactory())
              .addLast("contentDecompressor", new HttpContentDecompressor)
              .addLast("aggregator", new HttpObjectAggregator(maxContentLength))
          }
        })

    val serverBootstrap = new ServerBootstrap()
      .option(ChannelOption.SO_BACKLOG, Integer.valueOf(1024))
      .group(serverBossEventLoopGroup, serverWorkerEventLoopGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[Channel] {
        override def initChannel(ch: Channel): Unit = {
          logger.debug("Open new server channel")
          ch.pipeline
            .addLast("requestDecoder", new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize))
            .addLast("contentDecompressor", new HttpContentDecompressor)
            .addLast("responseEncoder", new HttpResponseEncoder)
            .addLast("contentCompressor", new HttpContentCompressor)
            .addLast("aggregator", new HttpObjectAggregator(maxContentLength))
            .addLast(
              GatlingServerHandler,
              new ServerHandler(actorSystem, outgoingProxy, clientBootstrap, sslServerContext, trafficLogger, httpClientCodecFactory, clock)
            )
        }
      })

    serverChannelGroup.add(serverBootstrap.bind(new InetSocketAddress(config.proxy.port)).sync.channel)

    new Mitm(
      serverChannelGroup,
      clientEventLoopGroup,
      serverBossEventLoopGroup,
      serverWorkerEventLoopGroup,
      actorSystem
    )
  }
}

class Mitm(
    serverChannelGroup: ChannelGroup,
    clientEventLoopGroup: EventLoopGroup,
    serverBossEventLoopGroup: EventLoopGroup,
    serverWorkerEventLoopGroup: EventLoopGroup,
    actorSystem: ActorSystem
) {

  def shutdown(): Unit = {
    clientEventLoopGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    serverChannelGroup.close.awaitUninterruptibly
    serverBossEventLoopGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    serverWorkerEventLoopGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    Await.ready(actorSystem.terminate(), 2.seconds)
  }
}
