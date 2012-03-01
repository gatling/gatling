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
import com.excilys.ebi.gatling.core.action.{ RequestAction, Action }
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandler
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.ning.http.client.{ AsyncHttpClientConfig, AsyncHttpClient }
import com.ning.http.client.Response
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder.status
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.GatlingHTTPConfig._
import akka.actor.ActorRef
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.config.ProtocolConfiguration
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.core.check.CheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.StatusReceived
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
	lazy val CLIENT = buildAsyncHttpClient

	protected def buildAsyncHttpClient = {

		val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
			.setCompressionEnabled(GATLING_HTTP_CONFIG_COMPRESSION_ENABLED)
			.setConnectionTimeoutInMs(GATLING_HTTP_CONFIG_CONNECTION_TIMEOUT)
			.setRequestTimeoutInMs(GATLING_HTTP_CONFIG_REQUEST_TIMEOUT)
			.setMaxRequestRetry(GATLING_HTTP_CONFIG_MAX_RETRY)
			.setAllowPoolingConnection(GATLING_HTTP_CONFIG_ALLOW_POOLING_CONNECTION)
			.build

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
 * @param next the next action that will be executed
 * @param givenCheckBuilders all the checks that will be performed on the response
 * @param feeder the feeder that will be consumed each time the request will be sent
 */
class HttpRequestAction(next: ActorRef, request: HttpRequest, givenChecks: Option[List[HttpCheck]], protocolConfiguration: Option[HttpProtocolConfiguration])
		extends RequestAction[HttpCheck, Response, HttpProtocolConfiguration](next, request, givenChecks, protocolConfiguration) with Logging {

	val checks = givenChecks match {
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
		HttpRequestAction.CLIENT.executeRequest(ahcRequest, new GatlingAsyncHandler(session, checks, next, request.name, ahcRequest, followRedirect))
	}
}
