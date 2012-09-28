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

import com.excilys.ebi.gatling.core.action.{ Action, Bypass, system }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.ahc.{ GatlingAsyncHandler, GatlingAsyncHandlerActor }
import com.excilys.ebi.gatling.http.cache.CacheHandling
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.referer.RefererHandling
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder
import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig }

import akka.actor.{ ActorRef, Props }
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
			.setUseRawUrl(configuration.http.userRawUrl)
			.build

		val providerClassName = "com.ning.http.client.providers." + configuration.http.provider.toLowerCase + "." + configuration.http.provider + "AsyncHttpProvider"

		val client = new AsyncHttpClient(providerClassName, ahcConfigBuilder)

		system.registerOnTermination(client.close)

		client
	}

	def apply(requestName: String, next: ActorRef, requestBuilder: AbstractHttpRequestBuilder[_], checks: List[HttpCheck[_]], protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {

		val httpConfig = protocolConfigurationRegistry.getProtocolConfiguration(HttpProtocolConfiguration.DEFAULT_HTTP_PROTOCOL_CONFIG)

		new HttpRequestAction(requestName, next, requestBuilder, checks, httpConfig)
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
class HttpRequestAction(requestName: String, next: ActorRef, requestBuilder: AbstractHttpRequestBuilder[_], checks: List[HttpCheck[_]], protocolConfiguration: HttpProtocolConfiguration) extends Action(requestName, next) with Bypass {

	val handlerFactory = GatlingAsyncHandler.newHandlerFactory(checks, protocolConfiguration)
	val asyncHandlerActorFactory = GatlingAsyncHandlerActor.newAsyncHandlerActorFactory(checks, next, requestName, protocolConfiguration)
	val client = HttpRequestAction.HTTP_CLIENT

	def execute(session: Session) {

		val request = requestBuilder.build(session, protocolConfiguration)
		val newSession = RefererHandling.storeReferer(request, session, protocolConfiguration)

		if (CacheHandling.isCached(protocolConfiguration, session, request)) {
			info("Bypassing cached Request '" + requestName + "': Scenario '" + session.scenarioName + "', UserId #" + session.userId)
			next ! newSession

		} else {
			info("Sending Request '" + requestName + "': Scenario '" + session.scenarioName + "', UserId #" + session.userId)

			val actor = context.actorOf(Props(asyncHandlerActorFactory(request, newSession)))
			val ahcHandler = handlerFactory(requestName, actor)
			client.executeRequest(request, ahcHandler)
		}
	}
}
