/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http

import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

import com.typesafe.scalalogging.LazyLogging

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.logging.{ Slf4JLoggerFactory, InternalLoggerFactory }

private[http] class HttpServer(requestHandler: PartialFunction[DefaultHttpRequest, ChannelHandlerContext => Unit], port: Int)
    extends LazyLogging {

  val requests = new ConcurrentLinkedQueue[DefaultHttpRequest]

  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  val serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory)

  serverBootstrap.setPipelineFactory(new ChannelPipelineFactory {
    val serverHandler = new SimpleChannelUpstreamHandler {
      override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = e.getMessage match {
        case request: DefaultHttpRequest =>
          requests.add(request)
          if (requestHandler isDefinedAt request) requestHandler(request)(ctx)
          else {
            logger.error(s"Unhandled request $request")
            ctx.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
          }
        case msg => logger.error(s"Unknown message $msg")
      }
    }

    override def getPipeline = {
      val pipeline = Channels.pipeline
      pipeline.addLast("httpDecoder", new HttpServerCodec)
      pipeline.addLast("chunkAggregator", new HttpChunkAggregator(Int.MaxValue))
      pipeline.addLast("compressor", new HttpContentCompressor)
      pipeline.addLast("decompressor", new HttpContentDecompressor)
      pipeline.addLast("serverHandler", serverHandler)
      pipeline
    }
  })

  serverBootstrap.bind(new InetSocketAddress(port))

  def stop(): Unit = serverBootstrap.shutdown()
}
