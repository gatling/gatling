/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.http.channel;

import org.jboss.netty.bootstrap.{ ServerBootstrap, ClientBootstrap }
import org.jboss.netty.channel.Channels.pipeline
import org.jboss.netty.channel.socket.nio.{ NioServerSocketChannelFactory, NioClientSocketChannelFactory }
import org.jboss.netty.channel.{ ChannelPipelineFactory, ChannelPipeline, ChannelHandlerContext }
import org.jboss.netty.handler.codec.http.{ HttpResponseEncoder, HttpRequestDecoder, HttpRequest, HttpContentDecompressor, HttpContentCompressor, HttpClientCodec, HttpChunkAggregator }
import org.jboss.netty.handler.ssl.SslHandler

import com.excilys.ebi.gatling.recorder.config.ProxyConfig
import com.excilys.ebi.gatling.recorder.http.handler.{ ServerHttpResponseHandler, BrowserHttpsRequestHandler, BrowserHttpRequestHandler }
import com.excilys.ebi.gatling.recorder.http.ssl.{ SSLEngineFactory, FirstEventIsUnsecuredConnectSslHandler }

object BootstrapFactory {

	private val CHUNK_MAX_SIZE = 100 * 1024 * 1024; // 100Mo

	private val clientChannelFactory = new NioClientSocketChannelFactory

	private val serverChannelFactory = new NioServerSocketChannelFactory

	def newClientBootstrap(browserCtx: ChannelHandlerContext, browserRequest: HttpRequest, ssl: Boolean): ClientBootstrap = {
		val bootstrap = new ClientBootstrap(clientChannelFactory)
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			def getPipeline: ChannelPipeline = {
				val tmpPipeline = pipeline()

				if (ssl)
					tmpPipeline.addLast("ssl", new SslHandler(SSLEngineFactory.newClientSSLEngine))
				tmpPipeline.addLast("codec", new HttpClientCodec)
				tmpPipeline.addLast("inflater", new HttpContentDecompressor)
				tmpPipeline.addLast("aggregator", new HttpChunkAggregator(CHUNK_MAX_SIZE))
				tmpPipeline.addLast("gatling", new ServerHttpResponseHandler(browserCtx, browserRequest))

				tmpPipeline
			}
		})

		bootstrap.setOption("child.tcpNoDelay", true)
		bootstrap.setOption("child.keepAlive", true)

		bootstrap
	}

	def newServerBootstrap(proxyConfig: ProxyConfig, ssl: Boolean): ServerBootstrap = {

		val bootstrap = new ServerBootstrap(serverChannelFactory)

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			def getPipeline: ChannelPipeline = {
				val tmpPipeline = pipeline()
				if (ssl)
					tmpPipeline.addLast("ssl", new FirstEventIsUnsecuredConnectSslHandler(SSLEngineFactory.newServerSSLEngine))
				tmpPipeline.addLast("decoder", new HttpRequestDecoder)
				tmpPipeline.addLast("aggregator", new HttpChunkAggregator(CHUNK_MAX_SIZE))
				tmpPipeline.addLast("encoder", new HttpResponseEncoder)
				tmpPipeline.addLast("deflater", new HttpContentCompressor)
				if (ssl)
					tmpPipeline.addLast("gatling", new BrowserHttpsRequestHandler(proxyConfig))
				else
					tmpPipeline.addLast("gatling", new BrowserHttpRequestHandler(proxyConfig))

				tmpPipeline
			}
		})

		bootstrap.setOption("child.tcpNoDelay", true)
		bootstrap.setOption("child.keepAlive", true)

		bootstrap
	}
}
