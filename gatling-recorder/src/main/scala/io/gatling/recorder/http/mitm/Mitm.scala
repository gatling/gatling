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

package io.gatling.recorder.http.mitm

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import io.gatling.commons.model.Credentials
import io.gatling.commons.util.Clock
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.OutgoingProxy
import io.gatling.recorder.http.ssl.SslServerContext

import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.{ Bootstrap, ServerBootstrap }
import io.netty.channel.{ Channel, ChannelInitializer, ChannelOption, EventLoopGroup, MultiThreadIoEventLoopGroup }
import io.netty.channel.group.{ ChannelGroup, DefaultChannelGroup }
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.{ NioServerSocketChannel, NioSocketChannel }
import io.netty.handler.codec.http._
import io.netty.util.concurrent.GlobalEventExecutor

object Mitm extends StrictLogging {

  object HandlerName {
    val Ssl = "ssl"
    val HttpCodec = "http"
    val RecorderClient = "recorder-client"
    val RecorderServer = "recorder-server"
  }

  def apply(controller: RecorderController, clock: Clock, config: RecorderConfiguration): Mitm = {
    val serverChannelGroup = new DefaultChannelGroup("Gatling_Recorder", GlobalEventExecutor.INSTANCE)
    val clientEventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory)
    val bindEventLoopGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory)
    val socketEventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory)

    val trafficLogger = new TrafficLogger(controller)
    val sslServerContext = SslServerContext(config)
    val httpClientCodecFactory = () => new HttpClientCodec(10000, 20000, 8192)

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
        .group(clientEventLoopGroup)
        .channel(classOf[NioSocketChannel])
        .option(ChannelOption.TCP_NODELAY, java.lang.Boolean.TRUE)
        .handler(new ChannelInitializer[Channel] {
          override def initChannel(ch: Channel): Unit = {
            logger.debug("Open new client channel")
            ch.pipeline
              .addLast(HandlerName.HttpCodec, httpClientCodecFactory())
              .addLast("contentDecompressor", new HttpContentDecompressor)
              .addLast("aggregator", new HttpObjectAggregator(Int.MaxValue))
          }
        })

    val serverBootstrap = new ServerBootstrap()
      .group(bindEventLoopGroup, socketEventLoopGroup)
      .channel(classOf[NioServerSocketChannel])
      .option(ChannelOption.SO_BACKLOG, Integer.valueOf(1024))
      .childHandler(new ChannelInitializer[Channel] {
        override def initChannel(ch: Channel): Unit = {
          logger.debug("Open new server channel")
          ch.pipeline
            .addLast("requestDecoder", new HttpRequestDecoder(10000, 20000, 8192))
            .addLast("contentDecompressor", new HttpContentDecompressor)
            .addLast("responseEncoder", new HttpResponseEncoder)
            .addLast("contentCompressor", new HttpContentCompressor)
            .addLast("aggregator", new HttpObjectAggregator(Int.MaxValue))
            .addLast(
              HandlerName.RecorderServer,
              new ServerHandler(outgoingProxy, clientBootstrap, sslServerContext, trafficLogger, httpClientCodecFactory, clock)
            )
        }
      })

    serverChannelGroup.add(serverBootstrap.bind(new InetSocketAddress(config.proxy.port)).sync.channel)

    new Mitm(
      serverChannelGroup,
      clientEventLoopGroup,
      bindEventLoopGroup,
      socketEventLoopGroup
    )
  }
}

final class Mitm(
    serverChannelGroup: ChannelGroup,
    clientEventLoopGroup: EventLoopGroup,
    bindEventLoopGroup: EventLoopGroup,
    socketEventLoopGroup: EventLoopGroup
) {
  def shutdown(): Unit = {
    serverChannelGroup.close.awaitUninterruptibly
    clientEventLoopGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    bindEventLoopGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    socketEventLoopGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
  }
}
