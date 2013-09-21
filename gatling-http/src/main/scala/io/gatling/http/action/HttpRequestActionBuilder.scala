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
package io.gatling.http.action

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.ProtocolRegistry
import io.gatling.core.session.Expression
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.ahc.RequestFactory
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckOrder.Status
import io.gatling.http.check.status.HttpStatusCheckBuilder.status
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.AbstractHttpRequestBuilder
import io.gatling.http.response.ResponseTransformer

object HttpRequestActionBuilder {

	/**
	 * This is the default HTTP check used to verify that the response status is 2XX
	 */
	val DEFAULT_HTTP_STATUS_CHECK = status.find.in(Session => (200 to 210).success).build

	def apply(requestBuilder: AbstractHttpRequestBuilder[_]): HttpRequestActionBuilder = {
		import requestBuilder.httpAttributes._
		new HttpRequestActionBuilder(requestName, requestBuilder.build, checks, ignoreDefaultChecks, responseTransformer)
	}
}

/**
 * Builder for HttpRequestActionBuilder
 *
 * @constructor creates an HttpRequestActionBuilder
 * @param requestBuilder the builder for the request that will be sent
 * @param next the next action to be executed
 */
class HttpRequestActionBuilder(requestName: Expression[String], requestFactory: RequestFactory, checks: List[HttpCheck], ignoreDefaultChecks: Boolean, responseTransformer: Option[ResponseTransformer]) extends ActionBuilder {

	private[gatling] def build(next: ActorRef, protocolRegistry: ProtocolRegistry): ActorRef = {

		val httpProtocol = protocolRegistry.getProtocol(HttpProtocol.default)

		val totalChecks = if (ignoreDefaultChecks)
			httpProtocol.checks
		else
			httpProtocol.checks ::: checks

		val resolvedChecks = totalChecks
			.find(_.order == Status)
			.map(_ => checks)
			.getOrElse(HttpRequestActionBuilder.DEFAULT_HTTP_STATUS_CHECK :: checks)
			.sorted

		actor(new HttpRequestAction(requestName, next, requestFactory, resolvedChecks, responseTransformer, httpProtocol))
	}
}
