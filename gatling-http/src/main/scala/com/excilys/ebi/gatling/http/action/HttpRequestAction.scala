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
import org.jboss.netty.logging.{ Slf4JLoggerFactory, InternalLoggerFactory }

import com.excilys.ebi.gatling.core.action.RequestAction
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.action.HttpRequestAction.HTTP_CLIENT
import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandler
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder.status
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpConfig.{ GATLING_HTTP_CONFIG_REQUEST_TIMEOUT, GATLING_HTTP_CONFIG_PROVIDER_CLASS, GATLING_HTTP_CONFIG_MAX_RETRY, GATLING_HTTP_CONFIG_CONNECTION_TIMEOUT, GATLING_HTTP_CONFIG_COMPRESSION_ENABLED, GATLING_HTTP_CONFIG_ALLOW_POOLING_CONNECTION }
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpPhase.StatusReceived
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.ning.http.client.{ Response, AsyncHttpClientConfig, AsyncHttpClient }

import akka.actor.ActorRef
import grizzled.slf4j.Logging

/**
 * HttpRequestAction class companion
 */
object HttpRequestAction {

	/**
	 * This is the default HTTP check used to verify that the response status is 2XX
	 */
	val DEFAULT_HTTP_STATUS_CHECK = status.find.in(Session => (200 to 210)).build

	/**
	 * The HTTP client used to send the requests
	 */
	lazy val HTTP_CLIENT = buildAsyncHttpClient

	protected def buildAsyncHttpClient = {

		val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
			.setCompressionEnabled(GATLING_HTTP_CONFIG_COMPRESSION_ENABLED)
			.setConnectionTimeoutInMs(GATLING_HTTP_CONFIG_CONNECTION_TIMEOUT)
			.setRequestTimeoutInMs(GATLING_HTTP_CONFIG_REQUEST_TIMEOUT)
			.setMaxRequestRetry(GATLING_HTTP_CONFIG_MAX_RETRY)
			.setAllowPoolingConnection(GATLING_HTTP_CONFIG_ALLOW_POOLING_CONNECTION)
			.build

		// set up Netty LoggerFactory for slf4j instead of default JDK
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

		val client = new AsyncHttpClient(GATLING_HTTP_CONFIG_PROVIDER_CLASS, ahcConfigBuilder)

		// Register client shutdown
		ResourceRegistry.registerOnCloseCallback(() => client.close)

		client
	}
}

/**
 * This is an action that sends HTTP requests
 *
 * @constructor constructs an HttpRequestAction
 * @param next the next action that will be executed after the request
 * @param request the request that will be executed
 * @param checks the checks that will be performed on the response
 * @param protocolConfiguration the protocol specific configuration
 */
class HttpRequestAction(next: ActorRef, request: HttpRequest, checks: Option[List[HttpCheck]], protocolConfiguration: Option[HttpProtocolConfiguration])
		extends RequestAction[HttpCheck, Response, HttpProtocolConfiguration](next, request, checks, protocolConfiguration) with Logging {

	val resolvedChecks = checks match {
		case Some(givenChecksContent) =>
			if (givenChecksContent.find(_.phase == StatusReceived).isEmpty) {
				// add default HttpStatusCheck if none was set
				HttpRequestAction.DEFAULT_HTTP_STATUS_CHECK :: givenChecksContent
			} else {
				givenChecksContent
			}
		case None => Nil
	}

	def execute(session: Session) = {
		info("Sending Request '" + request.name + "': Scenario '" + session.scenarioName + "', UserId #" + session.userId)

		val followRedirect = protocolConfiguration match {
			case Some(protocolConfiguration) => protocolConfiguration.followRedirect
			case None => false
		}
		val ahcRequest = request.buildAHCRequest(session, protocolConfiguration)
		val ahcHandler = new GatlingAsyncHandler(session, resolvedChecks, next, request.name, ahcRequest, followRedirect)
		HTTP_CLIENT.executeRequest(ahcRequest, ahcHandler)
	}
}
