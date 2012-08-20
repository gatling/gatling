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
package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.{ system, Action }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.action.HttpRequestAction.HTTP_CLIENT
import com.excilys.ebi.gatling.http.ahc.{ HandlerFactory, GatlingAsyncHandlerActor, GatlingAsyncHandler }
import com.excilys.ebi.gatling.http.cache.CacheHandling
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpConfig
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.referer.RefererHandling
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder
import com.excilys.ebi.gatling.http.response.ExtendedResponseBuilder
import com.ning.http.client.{ AsyncHttpClientConfig, AsyncHttpClient }
import akka.actor.{ Props, ActorRef }
import grizzled.slf4j.Logging

/**
 * HttpRequestAction class companion
 */
object HttpRequestAction extends Logging {

	/**
	 * The HTTP client used to send the requests
	 */
	val HTTP_CLIENT = {
		// set up Netty LoggerFactory for slf4j instead of default JDK
		try {
			val nettyInternalLoggerFactoryClass = Class.forName("org.jboss.netty.logging.InternalLoggerFactory")
			val nettySlf4JLoggerFactoryInstance = Class.forName("org.jboss.netty.logging.Slf4JLoggerFactory").newInstance
			val setDefaultFactoryMethod = nettyInternalLoggerFactoryClass.getMethod("setDefaultFactory", nettyInternalLoggerFactoryClass)
			setDefaultFactoryMethod.invoke(null, nettySlf4JLoggerFactoryInstance.asInstanceOf[AnyRef])

		} catch {
			case e => logger.info("Netty logger wasn't set up")
		}

		val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
			.setAllowPoolingConnection(HttpConfig.GATLING_HTTP_CONFIG_ALLOW_POOLING_CONNECTION)
			.setAllowSslConnectionPool(HttpConfig.GATLING_HTTP_CONFIG_ALLOW_SSL_CONNECTION_POOL)
			.setCompressionEnabled(HttpConfig.GATLING_HTTP_CONFIG_COMPRESSION_ENABLED)
			.setConnectionTimeoutInMs(HttpConfig.GATLING_HTTP_CONFIG_CONNECTION_TIMEOUT)
			.setIdleConnectionInPoolTimeoutInMs(HttpConfig.GATLING_HTTP_CONFIG_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS)
			.setIdleConnectionTimeoutInMs(HttpConfig.GATLING_HTTP_CONFIG_IDLE_CONNECTION_TIMEOUT_IN_MS)
			.setIOThreadMultiplier(HttpConfig.GATLING_HTTP_CONFIG_IO_THREAD_MULTIPLIER)
			.setMaximumConnectionsPerHost(HttpConfig.GATLING_HTTP_MAXIMUM_CONNECTIONS_PER_HOST)
			.setMaximumConnectionsTotal(HttpConfig.GATLING_HTTP_MAXIMUM_CONNECTIONS_TOTAL)
			.setMaxRequestRetry(HttpConfig.GATLING_HTTP_CONFIG_MAX_RETRY)
			.setRequestCompressionLevel(HttpConfig.GATLING_HTTP_CONFIG_REQUEST_COMPRESSION_LEVEL)
			.setRequestTimeoutInMs(HttpConfig.GATLING_HTTP_CONFIG_REQUEST_TIMEOUT_IN_MS)
			.setUseProxyProperties(HttpConfig.GATLING_HTTP_CONFIG_USE_PROXY_PROPERTIES)
			.setUserAgent(HttpConfig.GATLING_HTTP_CONFIG_USER_AGENT)
			.setUseRawUrl(HttpConfig.GATLING_HTTP_CONFIG_USE_RAW_URL)
			.build

		val client = new AsyncHttpClient(HttpConfig.GATLING_HTTP_CONFIG_PROVIDER_CLASS, ahcConfigBuilder)

		system.registerOnTermination(client.close)

		client
	}
}

/**
 * This is an action that sends HTTP requests
 *
 * @constructor constructs an HttpRequestAction
 * @param requestName the name of the request
 * @param next the next action that will be executed after the request
 * @param requestBuilder the builder for the request that will be executed
 * @param checks the checks that will be performed on the response
 * @param protocolConfiguration the protocol specific configuration
 */
class HttpRequestAction(requestName: String, next: ActorRef, requestBuilder: AbstractHttpRequestBuilder[_], checks: List[HttpCheck[_]], protocolConfiguration: Option[HttpProtocolConfiguration], gatlingConfiguration: GatlingConfiguration)
		extends Action with Logging {

	val handlerFactory = GatlingAsyncHandler.newHandlerFactory(checks)
	val asyncHandlerActorFactory = GatlingAsyncHandlerActor.newAsyncHandlerActorFactory(checks, next, requestName, protocolConfiguration, gatlingConfiguration)
	val client = HTTP_CLIENT
	val caching = protocolConfiguration.map(_.caching).getOrElse(true)

	def execute(session: Session) {

		val request = requestBuilder.build(session, protocolConfiguration)
		val newSession = RefererHandling.storeReferer(request, session, protocolConfiguration)

		if (caching && CacheHandling.isCached(session, request)) {
			info("Bypassing cached Request '" + requestName + "': Scenario '" + session.scenarioName + "', UserId #" + session.userId)
			next ! newSession

		} else {
			info("Sending Request '" + requestName + "': Scenario '" + session.scenarioName + "', UserId #" + session.userId)

			val actor = context.actorOf(Props(asyncHandlerActorFactory(request, newSession)))
			val ahcHandler = handlerFactory(requestName, actor)
			client.executeRequest(request, ahcHandler)
		}
	}

	override def preRestart(reason: Throwable, message: Option[Any]) {
		error("request " + requestName + " building crashed, skipping it", reason)
		message match {
			case Some(session: Session) => next ! session
			case _ =>
		}
	}
}
