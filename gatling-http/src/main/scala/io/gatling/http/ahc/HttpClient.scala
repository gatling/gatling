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

import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, Request }
import com.typesafe.scalalogging.slf4j.Logging

import akka.actor.ActorRef
import io.gatling.core.ConfigurationConstants._
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.controller.{ Controller, ThrottledRequest }
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.check.HttpCheck
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.HttpRequest
import io.gatling.http.response.ResponseBuilderFactory
import io.gatling.http.util.SSLHelper.{ RichAsyncHttpClientConfigBuilder, newKeyManagers, newTrustManagers }

case class HttpTx(session: Session,
	request: Request,
	requestName: String,
	checks: List[HttpCheck],
	responseBuilderFactory: ResponseBuilderFactory,
	protocol: HttpProtocol,
	next: ActorRef,
	maxRedirects: Option[Int],
	throttled: Boolean,
	explicitResources: Seq[HttpRequest],
	resourceFetching: Boolean = false,
	redirectCount: Int = 0)

object HttpClient extends AkkaDefaults with Logging {

	val applicationThreadPool = Executors.newCachedThreadPool(new ThreadFactory {
		override def newThread(r: Runnable) = {
			val t = new Thread(r, "Netty Thread")
			t.setDaemon(true)
			t
		}
	})

	val reaper = Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors, new ThreadFactory {
		override def newThread(r: Runnable): Thread = {
			val t = new Thread(r, "AsyncHttpClient-Reaper")
			t.setDaemon(true)
			t
		}
	})

	val provider = HttpProvider(applicationThreadPool)

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
			.setExecutorService(applicationThreadPool)
			.setScheduledExecutorService(reaper)
			.setAsyncHttpClientProviderConfig(provider.config)
			.setConnectionsPool(provider.connectionsPool)
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

			trustManagers.orElse(keyManagers).map { _ =>
				logger.info(s"Setting a custom SSLContext for user ${session.userId}")
				new AsyncHttpClientConfig.Builder(defaultAhcConfig).setSSLContext(trustManagers, keyManagers).build
			}

		}.getOrElse(defaultAhcConfig)

		val client = provider.newAsyncHttpClient(ahcConfig)
		system.registerOnTermination(client.close)
		client
	}

	lazy val default = newClient(None)

	val httpClientAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.client"

	def startHttpTransaction(tx: HttpTx) {

		val (newTx, httpClient) = if (tx.protocol.shareClient)
			(tx, default)
		else
			tx.session(httpClientAttributeName).asOption[AsyncHttpClient] match {
				case Some(client) => (tx, client)
				case _ =>
					val httpClient = newClient(tx.session)
					(tx.copy(session = tx.session.set(httpClientAttributeName, httpClient)), httpClient)

			}

		if (tx.throttled)
			Controller.controller ! ThrottledRequest(tx.session.scenarioName, () => httpClient.executeRequest(newTx.request, new AsyncHandler(newTx)))
		else
			httpClient.executeRequest(newTx.request, new AsyncHandler(newTx))
	}
}
