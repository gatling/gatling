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
package io.gatling.http.ahc

import java.util.concurrent.{ Executors, ThreadFactory }

import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.logging.{ InternalLoggerFactory, Slf4JLoggerFactory }

import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig }
import com.ning.http.client.providers.netty.{ NettyAsyncHttpProviderConfig, NettyConnectionsPool }
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.ConfigurationConstants._
import io.gatling.core.action.system
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.util.SSLHelper.{ RichAsyncHttpClientConfigBuilder, newKeyManagers, newTrustManagers }

object HttpClient extends Logging {

	// set up Netty LoggerFactory for slf4j instead of default JDK
	InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

	object SharedResources {

		val reaper = Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors, new ThreadFactory {
			override def newThread(r: Runnable): Thread = {
				val t = new Thread(r, "AsyncHttpClient-Reaper")
				t.setDaemon(true)
				t
			}
		})

		val applicationThreadPool = Executors.newCachedThreadPool(new ThreadFactory {
			override def newThread(r: Runnable) = {
				val t = new Thread(r, "Netty Thread")
				t.setDaemon(true)
				t
			}
		})

		val connectionsPool = new NettyConnectionsPool(configuration.http.ahc.maximumConnectionsTotal,
			configuration.http.ahc.maximumConnectionsPerHost,
			configuration.http.ahc.idleConnectionInPoolTimeOutInMs,
			configuration.http.ahc.maxConnectionLifeTimeInMs,
			configuration.http.ahc.allowSslConnectionPool)

		val nettyConfig = {
			val numWorkers = configuration.http.ahc.ioThreadMultiplier * Runtime.getRuntime.availableProcessors
			val socketChannelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool, applicationThreadPool, numWorkers)
			system.registerOnTermination(socketChannelFactory.releaseExternalResources)
			new NettyAsyncHttpProviderConfig().addProperty(NettyAsyncHttpProviderConfig.SOCKET_CHANNEL_FACTORY, socketChannelFactory)
		}
	}

	val defaultAhcConfig = {
		val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
			.setAllowPoolingConnection(configuration.http.ahc.allowPoolingConnection)
			.setAllowSslConnectionPool(configuration.http.ahc.allowSslConnectionPool)
			.setCompressionEnabled(configuration.http.ahc.compressionEnabled)
			.setConnectionTimeoutInMs(configuration.http.ahc.connectionTimeOut)
			.setIdleConnectionInPoolTimeoutInMs(configuration.http.ahc.idleConnectionInPoolTimeOutInMs)
			.setIdleConnectionTimeoutInMs(configuration.http.ahc.idleConnectionTimeOutInMs)
			.setIOThreadMultiplier(configuration.http.ahc.ioThreadMultiplier)
			.setMaximumConnectionsPerHost(configuration.http.ahc.maximumConnectionsPerHost)
			.setMaximumConnectionsTotal(configuration.http.ahc.maximumConnectionsTotal)
			.setMaxRequestRetry(configuration.http.ahc.maxRetry)
			.setRequestCompressionLevel(configuration.http.ahc.requestCompressionLevel)
			.setRequestTimeoutInMs(configuration.http.ahc.requestTimeOutInMs)
			.setUseProxyProperties(configuration.http.ahc.useProxyProperties)
			.setUserAgent(configuration.http.ahc.userAgent)
			.setUseRawUrl(configuration.http.ahc.useRawUrl)
			.setExecutorService(SharedResources.applicationThreadPool)
			.setScheduledExecutorService(SharedResources.reaper)
			.setAsyncHttpClientProviderConfig(SharedResources.nettyConfig)
			.setConnectionsPool(SharedResources.connectionsPool)
			.setRfc6265CookieEncoding(configuration.http.ahc.rfc6265CookieEncoding)

		val trustManagers = configuration.http.ssl.trustStore
			.map(config => newTrustManagers(config.storeType, config.file, config.password, config.algorithm))

		val keyManagers = configuration.http.ssl.keyStore
			.map(config => newKeyManagers(config.storeType, config.file, config.password, config.algorithm))

		if (trustManagers.isDefined || keyManagers.isDefined)
			ahcConfigBuilder.setSSLContext(trustManagers, keyManagers)

		ahcConfigBuilder.build
	}

	def newClient(session: Session): AsyncHttpClient = newClient(Some(session))
	def newClient(session: Option[Session]) = {

		val ahcConfig = session.flatMap { session =>

			val trustManagers = for {
				file <- session(CONF_HTTP_SSL_TRUST_STORE_FILE).asOption[String]
				password <- session(CONF_HTTP_SSL_TRUST_STORE_PASSWORD).asOption[String]
				storeType = session(CONF_HTTP_SSL_TRUST_STORE_TYPE).asOption[String]
				algorithm = session(CONF_HTTP_SSL_TRUST_STORE_ALGORITHM).asOption[String]
			} yield newTrustManagers(storeType, file, password, algorithm)

			val keyManagers = for {
				file <- session(CONF_HTTP_SSL_KEY_STORE_FILE).asOption[String]
				password <- session(CONF_HTTP_SSL_KEY_STORE_PASSWORD).asOption[String]
				storeType = session(CONF_HTTP_SSL_KEY_STORE_TYPE).asOption[String]
				algorithm = session(CONF_HTTP_SSL_KEY_STORE_ALGORITHM).asOption[String]
			} yield newKeyManagers(storeType, file, password, algorithm)

			if (trustManagers.isDefined || keyManagers.isDefined) {
				logger.info(s"Setting a custom SSLContext for user ${session.userId}")
				Some(new AsyncHttpClientConfig.Builder(defaultAhcConfig).setSSLContext(trustManagers, keyManagers).build)
			} else
				None

		}.getOrElse(defaultAhcConfig)

		val client = new AsyncHttpClient(ahcConfig)
		system.registerOnTermination(client.close)
		client
	}

	lazy val default = newClient(None)
}
