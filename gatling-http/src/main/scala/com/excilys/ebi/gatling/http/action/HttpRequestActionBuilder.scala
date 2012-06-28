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

import com.excilys.ebi.gatling.core.action.builder.ActionBuilder
import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.{ ProtocolConfigurationRegistry, GatlingConfiguration }
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder.status
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpPhase.StatusReceived
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder

import akka.actor.{ Props, ActorRef }

object HttpRequestActionBuilder {

	/**
	 * This is the default HTTP check used to verify that the response status is 2XX
	 */
	val DEFAULT_HTTP_STATUS_CHECK = status.find.in(Session => 200 to 210).build
}

/**
 * Builder for HttpRequestActionBuilder
 *
 * @constructor creates an HttpRequestActionBuilder
 * @param requestBuilder the builder for the request that will be sent
 * @param next the next action to be executed
 * @param checks the checks to be applied on the response
 */
class HttpRequestActionBuilder(requestName: String, requestBuilder: AbstractHttpRequestBuilder[_], next: ActorRef, checks: List[HttpCheck[_]]) extends ActionBuilder {

	private[gatling] def withNext(next: ActorRef) = new HttpRequestActionBuilder(requestName, requestBuilder, next, checks)

	private[gatling] val resolvedChecks = checks.find(_.phase == StatusReceived) match {
		case None => HttpRequestActionBuilder.DEFAULT_HTTP_STATUS_CHECK :: checks
		case _ => checks
	}

	private[gatling] def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry): ActorRef = {
		val httpConfig = protocolConfigurationRegistry.getProtocolConfiguration[HttpProtocolConfiguration]
		system.actorOf(Props(new HttpRequestAction(requestName, next, requestBuilder, resolvedChecks, httpConfig, GatlingConfiguration.configuration)))
	}
}
