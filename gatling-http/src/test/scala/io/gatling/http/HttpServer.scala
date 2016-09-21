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
package io.gatling.http

import java.net.InetSocketAddress
import java.util.concurrent.{ TimeUnit, ConcurrentLinkedQueue }

import scala.collection.JavaConversions._

import com.typesafe.scalalogging.LazyLogging

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.logging.{ LoggingHandler, LogLevel }
import io.netty.util.ReferenceCountUtil
import io.netty.util.internal.logging.{ Slf4JLoggerFactory, InternalLoggerFactory }

@Sharable
private[http] class ServerHandler(
  requestHandler: PartialFunction[FullHttpRequest, ChannelHandlerContext => Unit],
  requests:       ConcurrentLinkedQueue[FullHttpRequest]
)
    extends ChannelInboundHandlerAdapter with LazyLogging {

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
    msg match {
      case request: FullHttpRequest =>
        requests.add(request)
        if (requestHandler isDefinedAt request) {
          requestHandler(request)(ctx)
        } else {
          logger.error(s"Unhandled request $request")
          ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
            .addListener(ChannelFutureListener.CLOSE)
        }
      case errorMsg =>
        logger.error(s"Unknown message $errorMsg")
        ReferenceCountUtil.release(errorMsg)
    }
  }
}

private[http] class HttpServer(requestHandler: PartialFunction[FullHttpRequest, ChannelHandlerContext => Unit], port: Int)
    extends LazyLogging {

  val requests = new ConcurrentLinkedQueue[FullHttpRequest]

  InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)

  val serverHandler = new ServerHandler(requestHandler, requests)
  val bossGroup = new NioEventLoopGroup(1)
  val workerGroup = new NioEventLoopGroup

  val serverBootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(classOf[NioServerSocketChannel])
    .handler(new LoggingHandler(LogLevel.DEBUG))
    .childHandler(new ChannelInitializer[Channel] {
      override def initChannel(ch: Channel): Unit =
        ch.pipeline
          .addLast("httpDecoder", new HttpServerCodec)
          .addLast("chunkAggregator", new HttpObjectAggregator(Int.MaxValue))
          .addLast("compressor", new HttpContentCompressor)
          .addLast("serverHandler", serverHandler)
    })

  val ch = serverBootstrap.bind(new InetSocketAddress(port)).sync.channel

  def stop(): Unit = {
    requests.foreach(ReferenceCountUtil.release)
    bossGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    workerGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
  }
}
