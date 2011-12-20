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

import scala.collection.mutable.{ HashSet => MHashSet }
import com.excilys.ebi.gatling.core.action.{ RequestAction, Action }
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandler
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.ning.http.client.{ AsyncHttpClientConfig, AsyncHttpClient }
import com.ning.http.client.Response
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder._
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheck
import com.excilys.ebi.gatling.http.config.GatlingHTTPConfig._

/**
 * HttpRequestAction class companion
 */
object HttpRequestAction {
	/**
	 * This is the default HTTP check used to verify that the response status is 2XX
	 */
	val DEFAULT_HTTP_STATUS_CHECK = status.in(200 to 210).build

	val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
		.setCompressionEnabled(GATLING_HTTP_CONFIG_COMPRESSION_ENABLED)
		.setConnectionTimeoutInMs(GATLING_HTTP_CONFIG_CONNECTION_TIMEOUT)
		.setRequestTimeoutInMs(GATLING_HTTP_CONFIG_REQUEST_TIMEOUT)
		.setMaxRequestRetry(GATLING_HTTP_CONFIG_MAX_RETRY)
		.setAllowPoolingConnection(GATLING_HTTP_CONFIG_ALLOW_POOLING_CONNECTION)
		.build

	/**
	 * The HTTP client used to send the requests
	 */
	val CLIENT: AsyncHttpClient = new AsyncHttpClient(GATLING_HTTP_CONFIG_PROVIDER_CLASS, ahcConfigBuilder)
	// Register client shutdown
	ResourceRegistry.registerOnCloseCallback(() => CLIENT.close)
}

/**
 * This is an action that sends HTTP requests
 *
 * @constructor constructs an HttpRequestAction
 * @param next the next action that will be executed
 * @param givenCheckBuilders all the checks that will be performed on the response
 * @param feeder the feeder that will be consumed each time the request will be sent
 */
class HttpRequestAction(next: Action, request: HttpRequest, givenCheckBuilders: Option[List[HttpCheckBuilder[_]]])
		extends RequestAction[Response](next, request, givenCheckBuilders) {

	var checks: List[HttpCheck] = Nil

	givenCheckBuilders.map {
		list =>
			checks = list.map(_.build)

			// add default HttpStatusCheck if none was set
			if (checks.find(_.isInstanceOf[HttpStatusCheck]).isEmpty) {
				checks = HttpRequestAction.DEFAULT_HTTP_STATUS_CHECK :: checks
			}
	}

	def execute(session: Session) = {

		if (logger.isInfoEnabled())
			logger.info("Sending Request '{}': Scenario '{}', UserId #{}", Array[Object](request.name, session.scenarioName, session.userId: Integer))

		HttpRequestAction.CLIENT.executeRequest(request.getRequest(session), new GatlingAsyncHandler(session, checks, next, request.name))
	}
}
