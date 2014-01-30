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
package io.gatling.http.ahc

import java.util.Timer
import java.util.concurrent.{ ExecutorService, Executors }

import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, AsyncHttpProviderConfig, ConnectionsPool }

import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.GatlingConfiguration.configuration

object HttpProvider {

	def apply(threadPool: ExecutorService): HttpProvider = new NettyProvider(threadPool)
}

sealed trait HttpProvider {

	def connectionsPool: ConnectionsPool[_, _]
	def config: AsyncHttpProviderConfig[_, _]
	def newAsyncHttpClient(config: AsyncHttpClientConfig): AsyncHttpClient
}

class NettyProvider(threadPool: ExecutorService) extends HttpProvider with AkkaDefaults {

	import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
	import org.jboss.netty.logging.{ InternalLoggerFactory, Slf4JLoggerFactory }

	import com.ning.http.client.providers.netty.{ NettyAsyncHttpProviderConfig, NettyConnectionsPool }

	// set up Netty LoggerFactory for slf4j instead of default JDK
	InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

	val connectionsPool = new NettyConnectionsPool(configuration.http.ahc.maximumConnectionsTotal,
		configuration.http.ahc.maximumConnectionsPerHost,
		configuration.http.ahc.idleConnectionInPoolTimeOutInMs,
		configuration.http.ahc.maxConnectionLifeTimeInMs,
		configuration.http.ahc.allowSslConnectionPool,
		new Timer(true))

	val config = {
		val numWorkers = configuration.http.ahc.ioThreadMultiplier * Runtime.getRuntime.availableProcessors
		val socketChannelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool, threadPool, numWorkers)
		system.registerOnTermination(socketChannelFactory.releaseExternalResources)
		new NettyAsyncHttpProviderConfig().addProperty(NettyAsyncHttpProviderConfig.SOCKET_CHANNEL_FACTORY, socketChannelFactory)
	}

	def newAsyncHttpClient(config: AsyncHttpClientConfig) = new AsyncHttpClient(config)
}
