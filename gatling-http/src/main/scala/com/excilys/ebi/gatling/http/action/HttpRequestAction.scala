/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandler
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.resource.HttpClientResource
import com.ning.http.client.{ AsyncHttpClientConfig, AsyncHttpClient }
import com.ning.http.client.Response
import com.excilys.ebi.gatling.http.capture.HttpCapture
import com.excilys.ebi.gatling.http.capture.HttpCaptureBuilder
import com.excilys.ebi.gatling.http.capture.status.check.HttpStatusCheck
import com.excilys.ebi.gatling.http.capture.status.check.HttpStatusCheckBuilder._

object HttpRequestAction {
	val DEFAULT_HTTP_STATUS_CHECK = statusInRange(Range(200, 210)).build
	
	// TODO lazy?
	val CLIENT: AsyncHttpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setCompressionEnabled(true).build())
	ResourceRegistry.register(new HttpClientResource(CLIENT))
}

class HttpRequestAction(next: Action, request: HttpRequest, givenCaptureBuilders: Option[List[HttpCaptureBuilder[_]]], groups: List[String], feeder: Option[Feeder])
		extends RequestAction[Response](next, request, givenCaptureBuilders, groups, feeder) {

	var captures = new MHashSet[HttpCapture]

	givenCaptureBuilders.map {
		list =>
			{
				var httpStatusCheckSet = false
				for (captureBuilder <- list) {
					val capture = captureBuilder.build
					httpStatusCheckSet = httpStatusCheckSet || capture.isInstanceOf[HttpStatusCheck]
					captures.add(capture)
					logger.debug("  -- Building {} with phase {}", capture, capture.when)
				}

				// add default HttpStatusCheck if none was set
				if (!httpStatusCheckSet) {
					captures.add(HttpRequestAction.DEFAULT_HTTP_STATUS_CHECK)
				}
			}
	}

	def execute(context: Context) = {

		if (logger.isInfoEnabled()) {
			logger.info("Sending Request '{}': Scenario '{}', UserId #{}", Array(request.name, context.getScenarioName, context.getUserId.toString))
		}

		feeder.map {
			feeder => context.setAttributes(feeder.next)
		}

		HttpRequestAction.CLIENT.executeRequest(request.getRequest(context), new GatlingAsyncHandler(context, captures, next, request.getName, groups))
	}
}
