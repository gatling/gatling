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
package io.gatling.recorder.http.channel

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.handler.remote.TimedHttpRequest
import io.gatling.recorder.http.handler.user.PortUnificationUserHandler
import io.gatling.recorder.http.ssl.SslClientContext

import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{ NioSocketChannel, NioServerSocketChannel }
import io.netty.handler.codec.http.{ HttpContentDecompressor, HttpContentCompressor, HttpObjectAggregator }
import io.netty.bootstrap.{ Bootstrap, ServerBootstrap }
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler
import com.typesafe.scalalogging.StrictLogging
import io.netty.util.AttributeKey

private[http] object BootstrapFactory extends StrictLogging {

  val CodecHandlerName = "codec"
  val SslHandlerSetterName = "ssl-setter"
  val SslHandlerName = "ssl"
  val GatlingHandlerName = "gatling"
  val PortUnificationServerHandler = "port-unification"
  val TimedHttpRequestAttribute: AttributeKey[TimedHttpRequest] = AttributeKey.valueOf("default")

  def newRemoteBootstrap(clientGroup: NioEventLoopGroup, ssl: Boolean, config: RecorderConfiguration): Bootstrap = {

    import config.netty._

    new Bootstrap().channel(classOf[NioSocketChannel])
      .group(clientGroup)
      .handler(new ChannelInitializer[Channel] {
        override def initChannel(ch: Channel): Unit = {
          logger.debug("Open new remote channel")
          val pipeline = ch.pipeline
          if (ssl)
            pipeline.addLast(SslHandlerName, new SslHandler(SslClientContext.createSSLEngine))
          pipeline
            .addLast(CodecHandlerName, new HttpClientCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize))
            .addLast("inflater", new HttpContentDecompressor)
            .addLast("aggregator", new HttpObjectAggregator(maxContentLength))
        }
      })
  }

  def newUserBootstrap(serverBossGroup: NioEventLoopGroup, serverWorkerGroup: NioEventLoopGroup, proxy: HttpProxy, config: RecorderConfiguration): ServerBootstrap = {

    import config.netty._

    new ServerBootstrap()
      .option(ChannelOption.SO_BACKLOG, Integer.valueOf(1024))
      .option(ChannelOption.TCP_NODELAY, java.lang.Boolean.TRUE)
      .group(serverBossGroup, serverWorkerGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[Channel] {
        override def initChannel(ch: Channel): Unit = {
          logger.debug("Open new user channel")
          val pipeline = ch.pipeline
          pipeline
            .addLast("decoder", new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize))
            .addLast("inflater", new HttpContentDecompressor)
            .addLast("encoder", new HttpResponseEncoder)
            .addLast("deflater", new HttpContentCompressor)
            .addLast("aggregator", new HttpObjectAggregator(maxContentLength))
            .addLast(PortUnificationServerHandler, new PortUnificationUserHandler(proxy, pipeline))
        }
      })
  }
}
