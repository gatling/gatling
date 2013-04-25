/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import org.jboss.netty.bootstrap.{ ServerBootstrap, ClientBootstrap }
import org.jboss.netty.channel.{ ChannelPipelineFactory, ChannelPipeline, ChannelHandlerContext }
import org.jboss.netty.channel.Channels
import org.jboss.netty.channel.socket.nio.{ NioServerSocketChannelFactory, NioClientSocketChannelFactory }
import org.jboss.netty.handler.codec.http.{ HttpResponseEncoder, HttpRequestDecoder, HttpRequest, HttpContentDecompressor, HttpContentCompressor, HttpClientCodec, HttpChunkAggregator }
import org.jboss.netty.handler.ssl.SslHandler

import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.handler.{ ServerHttpResponseHandler, BrowserHttpsRequestHandler, BrowserHttpRequestHandler }
import io.gatling.recorder.http.ssl.{ SSLEngineFactory, FirstEventIsUnsecuredConnectSslHandler }

object BootstrapFactory {

	val SSL_HANDLER_NAME = "ssl"

	private val CHUNK_MAX_SIZE = 100 * 1024 * 1024; // 100Mo

	private val clientChannelFactory = new NioClientSocketChannelFactory

	private val serverChannelFactory = new NioServerSocketChannelFactory

	def newClientBootstrap(controller: RecorderController, requestContext: ChannelHandlerContext, browserRequest: HttpRequest, ssl: Boolean): ClientBootstrap = {
		val bootstrap = new ClientBootstrap(clientChannelFactory)
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			def getPipeline: ChannelPipeline = {
				val pipeline = Channels.pipeline

				if (ssl)
					pipeline.addLast(SSL_HANDLER_NAME, new SslHandler(SSLEngineFactory.newClientSSLEngine))
				pipeline.addLast("codec", new HttpClientCodec)
				pipeline.addLast("inflater", new HttpContentDecompressor)
				pipeline.addLast("aggregator", new HttpChunkAggregator(CHUNK_MAX_SIZE))
				pipeline.addLast("gatling", new ServerHttpResponseHandler(controller, requestContext, browserRequest))

				pipeline
			}
		})

		bootstrap.setOption("child.tcpNoDelay", true)
		bootstrap.setOption("child.keepAlive", true)

		bootstrap
	}

	def newServerBootstrap(controller: RecorderController, ssl: Boolean): ServerBootstrap = {

		val bootstrap = new ServerBootstrap(serverChannelFactory)

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			def getPipeline: ChannelPipeline = {
				val pipeline = Channels.pipeline
				if (ssl)
					pipeline.addLast(SSL_HANDLER_NAME, new FirstEventIsUnsecuredConnectSslHandler(SSLEngineFactory.newServerSSLEngine))
				pipeline.addLast("decoder", new HttpRequestDecoder)
				pipeline.addLast("aggregator", new HttpChunkAggregator(CHUNK_MAX_SIZE))
				pipeline.addLast("encoder", new HttpResponseEncoder)
				pipeline.addLast("deflater", new HttpContentCompressor)
				if (ssl)
					pipeline.addLast("gatling", new BrowserHttpsRequestHandler(controller))
				else
					pipeline.addLast("gatling", new BrowserHttpRequestHandler(controller))

				pipeline
			}
		})

		bootstrap.setOption("child.tcpNoDelay", true)
		bootstrap.setOption("child.keepAlive", true)

		bootstrap
	}
}
