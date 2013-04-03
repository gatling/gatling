/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.io.{ File, FileInputStream }
import java.security.{ KeyStore, SecureRandom }

import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig }
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.action.system
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.IOHelper.withCloseable
import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }

object GatlingHttpClient extends Logging {

	/**
	 * The HTTP client used to send the requests
	 */
	val client = {
		// set up Netty LoggerFactory for slf4j instead of default JDK
		try {
			val nettyInternalLoggerFactoryClass = Class.forName("org.jboss.netty.logging.InternalLoggerFactory")
			val nettySlf4JLoggerFactoryInstance = Class.forName("org.jboss.netty.logging.Slf4JLoggerFactory").newInstance
			val setDefaultFactoryMethod = nettyInternalLoggerFactoryClass.getMethod("setDefaultFactory", nettyInternalLoggerFactoryClass)
			setDefaultFactoryMethod.invoke(null, nettySlf4JLoggerFactoryInstance.asInstanceOf[AnyRef])

		} catch {
			case e: Exception => logger.info("Netty logger wasn't set up", e)
		}

		val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
			.setAllowPoolingConnection(configuration.http.allowPoolingConnection)
			.setAllowSslConnectionPool(configuration.http.allowSslConnectionPool)
			.setCompressionEnabled(configuration.http.compressionEnabled)
			.setConnectionTimeoutInMs(configuration.http.connectionTimeOut)
			.setIdleConnectionInPoolTimeoutInMs(configuration.http.idleConnectionInPoolTimeOutInMs)
			.setIdleConnectionTimeoutInMs(configuration.http.idleConnectionTimeOutInMs)
			.setIOThreadMultiplier(configuration.http.ioThreadMultiplier)
			.setMaximumConnectionsPerHost(configuration.http.maximumConnectionsPerHost)
			.setMaximumConnectionsTotal(configuration.http.maximumConnectionsTotal)
			.setMaxRequestRetry(configuration.http.maxRetry)
			.setRequestCompressionLevel(configuration.http.requestCompressionLevel)
			.setRequestTimeoutInMs(configuration.http.requestTimeOutInMs)
			.setUseProxyProperties(configuration.http.useProxyProperties)
			.setUserAgent(configuration.http.userAgent)
			.setUseRawUrl(configuration.http.useRawUrl)

		if (configuration.http.ssl.trustStore.isDefined || configuration.http.ssl.keyStore.isDefined) {

			val trustManagers = configuration.http.ssl.trustStore.map { trustStoreConfig =>
				withCloseable(new FileInputStream(new File(trustStoreConfig.file))) { is =>
					val trustStore = KeyStore.getInstance(trustStoreConfig.storeType)
					trustStore.load(is, trustStoreConfig.password.toCharArray)
					val algo = trustStoreConfig.algorithm.getOrElse(KeyManagerFactory.getDefaultAlgorithm)
					val tmf = TrustManagerFactory.getInstance(algo)
					tmf.init(trustStore)
					tmf.getTrustManagers
				}
			}.getOrElse(null)

			val keyManagers = configuration.http.ssl.keyStore.map { keyStoreConfig =>
				withCloseable(new FileInputStream(new File(keyStoreConfig.file))) { is =>
					val keyStore = KeyStore.getInstance(keyStoreConfig.storeType)
					keyStore.load(is, keyStoreConfig.password.toCharArray)
					val algo = keyStoreConfig.algorithm.getOrElse(KeyManagerFactory.getDefaultAlgorithm)
					val kmf = KeyManagerFactory.getInstance(algo)
					kmf.init(keyStore, keyStoreConfig.password.toCharArray)
					kmf.getKeyManagers
				}
			}.getOrElse(null)

			val sslContext = SSLContext.getInstance("TLS")
			sslContext.init(keyManagers, trustManagers, new SecureRandom)
			ahcConfigBuilder.setSSLContext(sslContext)
		}

		val ahcConfig = ahcConfigBuilder.build

		val providerClassName = "com.ning.http.client.providers." + configuration.http.provider.toLowerCase + "." + configuration.http.provider + "AsyncHttpProvider"

		val client = new AsyncHttpClient(providerClassName, ahcConfig)

		system.registerOnTermination(client.close)

		client
	}
}
