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
package io.gatling.recorder.http.channel

import org.jboss.netty.bootstrap.{ ClientBootstrap, ServerBootstrap }
import org.jboss.netty.channel.{ ChannelPipeline, ChannelPipelineFactory, Channels }
import org.jboss.netty.channel.socket.nio.{ NioClientSocketChannelFactory, NioServerSocketChannelFactory }
import org.jboss.netty.handler.codec.http.{ HttpChunkAggregator, HttpClientCodec, HttpContentCompressor, HttpContentDecompressor, HttpRequestDecoder, HttpResponseEncoder }
import org.jboss.netty.handler.ssl.SslHandler
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.handler.{ ClientPortUnifiedRequestHandler, ClientRequestHandler }
import io.gatling.recorder.http.ssl.SSLEngineFactory

object BootstrapFactory extends StrictLogging {

  val SslHandlerName = "ssl"
  val GatlingHandlerName = "gatling"
  val ConditionalHandlerName = "conditional"

  private val ChunkMaxSize = 100 * 1024 * 1024 // 100Mo

  def newClientBootstrap(ssl: Boolean): ClientBootstrap = {
    val bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory)
    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        logger.debug("Open new client channel")
        val pipeline = Channels.pipeline
        if (ssl)
          pipeline.addLast(SslHandlerName, new SslHandler(SSLEngineFactory.newClientSSLEngine))
        pipeline.addLast("codec", new HttpClientCodec)
        pipeline.addLast("inflater", new HttpContentDecompressor)
        pipeline.addLast("aggregator", new HttpChunkAggregator(ChunkMaxSize))
        pipeline
      }
    })

    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)

    bootstrap
  }

  def newServerBootstrap(proxy: HttpProxy): ServerBootstrap = {

    val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory)

    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        logger.debug("Open new server channel")
        val pipeline = Channels.pipeline
        pipeline.addLast("decoder", new HttpRequestDecoder)
        pipeline.addLast("aggregator", new HttpChunkAggregator(ChunkMaxSize))
        pipeline.addLast("encoder", new HttpResponseEncoder)
        pipeline.addLast("deflater", new HttpContentCompressor)
        pipeline.addLast(ConditionalHandlerName, new ClientPortUnifiedRequestHandler(proxy, pipeline))
        pipeline
      }
    })

    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)

    bootstrap
  }

  def upgradeProtocol(pipeline: ChannelPipeline): Unit = {
    pipeline.remove("codec")
    pipeline.addFirst("codec", new HttpClientCodec)
    pipeline.addFirst(SslHandlerName, new SslHandler(SSLEngineFactory.newClientSSLEngine))
  }

  def setGatlingProtocolHandler(pipeline: ChannelPipeline, handler: ClientRequestHandler): Unit = {
    pipeline.addLast(GatlingHandlerName, handler)
    pipeline.remove(ConditionalHandlerName)
  }
}
