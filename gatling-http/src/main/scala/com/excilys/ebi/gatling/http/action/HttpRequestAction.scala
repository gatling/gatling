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
package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.{ Action, Bypass }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.{ EvaluatableString, Session }
import com.excilys.ebi.gatling.http.ahc.{ GatlingAsyncHandler, GatlingAsyncHandlerActor, GatlingHttpClient }
import com.excilys.ebi.gatling.http.cache.CacheHandling
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.referer.RefererHandling
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder
import com.ning.http.client.AsyncHttpClient

import akka.actor.{ ActorRef, Props }
import grizzled.slf4j.Logging

/**
 * HttpRequestAction class companion
 */
object HttpRequestAction extends Logging {

	def apply(requestName: EvaluatableString, next: ActorRef, requestBuilder: AbstractHttpRequestBuilder[_], checks: List[HttpCheck[_]], protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {

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
class HttpRequestAction(requestName: EvaluatableString, val next: ActorRef, requestBuilder: AbstractHttpRequestBuilder[_], checks: List[HttpCheck[_]], protocolConfiguration: HttpProtocolConfiguration) extends Action with Bypass {

	val handlerFactory = GatlingAsyncHandler.newHandlerFactory(checks, protocolConfiguration)
	val asyncHandlerActorFactory = GatlingAsyncHandlerActor.newAsyncHandlerActorFactory(checks, next, protocolConfiguration) _

	def execute(session: Session) {

		val request = requestBuilder.build(session, protocolConfiguration)
		val resolvedRequestName = try {
			requestName(session)
		} catch {
			case e: Exception => error("Request name resolution crashed", e); "no-name"
		}
		val newSession = RefererHandling.storeReferer(request, session, protocolConfiguration)

		if (CacheHandling.isCached(protocolConfiguration, session, request)) {
			info("Bypassing cached Request '" + resolvedRequestName + "': Scenario '" + session.scenarioName + "', UserId #" + session.userId)
			next ! newSession

		} else {
			val (sessionWithClient, client) =
				if (protocolConfiguration.shareClient)
					(newSession, GatlingHttpClient.defaultClient)
				else
					newSession.getAttributeAsOption[AsyncHttpClient](GatlingHttpClient.httpClientAttributeName)
						.map((newSession, _))
						.getOrElse {
							val client = GatlingHttpClient.newClient(session)
							(newSession.setAttribute(GatlingHttpClient.httpClientAttributeName, client), client)
						}

			info("Sending Request '" + resolvedRequestName + "': Scenario '" + sessionWithClient.scenarioName + "', UserId #" + sessionWithClient.userId)
			val actor = context.actorOf(Props(asyncHandlerActorFactory(resolvedRequestName)(request, sessionWithClient)))
			val ahcHandler = handlerFactory(resolvedRequestName, actor)
			client.executeRequest(request, ahcHandler)
		}
	}
}
