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
/**
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
package io.gatling.http.action

import com.ning.http.client.{ AsyncHttpClient, Request }

import akka.actor.{ ActorRef, Props }
import io.gatling.core.action.Interruptable
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.Failure
import io.gatling.http.ahc.{ GatlingAsyncHandler, GatlingAsyncHandlerActor, GatlingHttpClient, RequestFactory }
import io.gatling.http.cache.CacheHandling
import io.gatling.http.check.HttpCheck
import io.gatling.http.config.HttpProtocolConfiguration
import io.gatling.http.referer.RefererHandling
import io.gatling.http.response.ResponseProcessor

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
class HttpRequestAction(
	requestName: Expression[String],
	val next: ActorRef,
	requestFactory: RequestFactory,
	checks: List[HttpCheck],
	responseProcessor: Option[ResponseProcessor],
	protocolConfiguration: HttpProtocolConfiguration) extends Interruptable {

	val handlerFactory = GatlingAsyncHandler.newHandlerFactory(checks, protocolConfiguration)
	val asyncHandlerActorFactory = GatlingAsyncHandlerActor.newAsyncHandlerActorFactory(checks, next, responseProcessor, protocolConfiguration) _

	def execute(session: Session) {

		def sendRequest(resolvedRequestName: String, request: Request, newSession: Session) = {

			if (CacheHandling.isCached(protocolConfiguration, newSession, request)) {
				logger.info(s"Skipping cached request '$resolvedRequestName': scenario '${newSession.scenarioName}', userId #${newSession.userId}")
				next ! newSession

			} else {
				val (sessionWithClient, client) =
					if (protocolConfiguration.shareConnections)
						(newSession, GatlingHttpClient.defaultClient)
					else
						newSession.get[AsyncHttpClient](GatlingHttpClient.httpClientAttributeName)
							.map((newSession, _))
							.getOrElse {
								val client = GatlingHttpClient.newClient
								(newSession.set(GatlingHttpClient.httpClientAttributeName, client), client)
							}

				logger.info(s"Sending request '$resolvedRequestName': scenario '${sessionWithClient.scenarioName}', userId #${sessionWithClient.userId}")
				val actor = context.actorOf(Props(asyncHandlerActorFactory(resolvedRequestName)(request, sessionWithClient)))
				val ahcHandler = handlerFactory(resolvedRequestName, actor)
				client.executeRequest(request, ahcHandler)
			}
		}

		val execution = for {
			resolvedRequestName <- requestName(session)
			request <- requestFactory(session, protocolConfiguration)
			newSession = RefererHandling.storeReferer(request, session, protocolConfiguration)

		} yield sendRequest(resolvedRequestName, request, newSession)

		execution match {
			case Failure(message) =>
				logger.error(message)
				next ! session
			case _ =>
		}
	}
}
