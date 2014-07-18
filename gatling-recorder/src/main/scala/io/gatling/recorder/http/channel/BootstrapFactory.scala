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

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.handler.server.PortUnificationServerHandler
import io.gatling.recorder.http.ssl.SSLEngineFactory
import org.jboss.netty.bootstrap.{ ClientBootstrap, ServerBootstrap }
import org.jboss.netty.channel.{ ChannelPipeline, ChannelPipelineFactory, Channels }
import org.jboss.netty.channel.socket.nio.{ NioClientSocketChannelFactory, NioServerSocketChannelFactory }
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.ssl.SslHandler
import com.typesafe.scalalogging.slf4j.StrictLogging

object BootstrapFactory extends StrictLogging {

  val CodecHandlerName = "codec"
  val SslHandlerName = "ssl"
  val GatlingHandlerName = "gatling"
  val ConditionalHandlerName = "conditional"

  def newClientBootstrap(ssl: Boolean, config: RecorderConfiguration): ClientBootstrap = {

    import config.netty._

    val bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory)
    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        logger.debug("Open new client channel")
        val pipeline = Channels.pipeline
        if (ssl)
          pipeline.addLast(SslHandlerName, new SslHandler(SSLEngineFactory.newClientSSLEngine))
        pipeline.addLast(CodecHandlerName, new HttpClientCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize))
        pipeline.addLast("inflater", new HttpContentDecompressor)
        pipeline.addLast("aggregator", new HttpChunkAggregator(maxContentLength))
        pipeline
      }
    })

    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)

    bootstrap
  }

  def newServerBootstrap(proxy: HttpProxy, config: RecorderConfiguration): ServerBootstrap = {

    import config.netty._

    val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory)

    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        logger.debug("Open new server channel")
        val pipeline = Channels.pipeline
        pipeline.addLast(CodecHandlerName, new HttpServerCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize))
        pipeline.addLast("deflater", new HttpContentCompressor)
        pipeline.addLast(ConditionalHandlerName, new PortUnificationServerHandler(proxy, pipeline))
        pipeline
      }
    })

    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)

    bootstrap
  }
}
