/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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
import java.util.concurrent.{ ConcurrentLinkedQueue, TimeUnit }

import scala.jdk.CollectionConverters._

import com.typesafe.scalalogging.LazyLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.{ HttpObjectAggregator, HttpServerCodec }
import io.netty.handler.codec.http.websocketx.{ TextWebSocketFrame, WebSocketServerProtocolHandler }

/**
 * A WebSocket server for tests.
 *
 * `scripted` turns an inbound text message into the messages to send back, so that a test can drive the exact sequence it wants to assert on.
 */
private[gatling] final class WebSocketServer(port: Int, path: String, subprotocol: Option[String], scripted: String => List[String]) extends LazyLogging {
  private val received = new ConcurrentLinkedQueue[String]

  def messages: List[String] = received.asScala.toList

  private val bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory)
  private val workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory)

  private val channel = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(classOf[NioServerSocketChannel])
    .childHandler(new ChannelInitializer[Channel] {
      override def initChannel(ch: Channel): Unit = {
        ch.pipeline
          .addLast(new HttpServerCodec)
          .addLast(new HttpObjectAggregator(Int.MaxValue))
          .addLast(new WebSocketServerProtocolHandler(path, subprotocol.orNull))
          .addLast(new SimpleChannelInboundHandler[TextWebSocketFrame] {
            override def channelRead0(ctx: ChannelHandlerContext, frame: TextWebSocketFrame): Unit = {
              val message = frame.text
              logger.debug(s"WebSocket server received $message")
              received.add(message)
              scripted(message).foreach(response => ctx.channel.writeAndFlush(new TextWebSocketFrame(response)))
            }
          })
        ()
      }
    })
    .bind(new InetSocketAddress(port))
    .sync
    .channel

  def stop(): Unit = {
    channel.close.sync
    bossGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    workerGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    ()
  }
}
