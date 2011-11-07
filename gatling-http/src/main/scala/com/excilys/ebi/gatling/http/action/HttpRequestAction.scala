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
import com.ning.http.client.{ AsyncHttpClientConfig, AsyncHttpClient }
import com.ning.http.client.Response
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder._
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheck

object HttpRequestAction {
	val DEFAULT_HTTP_STATUS_CHECK = statusInRange(Range(200, 210)).build

	val CLIENT: AsyncHttpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setCompressionEnabled(true).build())
	ResourceRegistry.registerOnCloseCallback(() => CLIENT.close)
}

class HttpRequestAction(next: Action, request: HttpRequest, givenCaptureBuilders: Option[List[HttpCheckBuilder[_]]], groups: List[String], feeder: Option[Feeder])
		extends RequestAction[Response](next, request, givenCaptureBuilders, groups, feeder) {

	var captures = new MHashSet[HttpCheck]

	givenCaptureBuilders.map {
		list =>
			{
				for (captureBuilder <- list) {
					val capture = captureBuilder.build
					captures.add(capture)
					logger.debug("  -- Building {} with phase {}", capture, capture.when)
				}

				// add default HttpStatusCheck if none was set
				if (captures.view.filter(_.isInstanceOf[HttpStatusCheck]).isEmpty) {
					captures.add(HttpRequestAction.DEFAULT_HTTP_STATUS_CHECK)
				}
			}
	}

	def execute(context: Context) = {

		if (logger.isInfoEnabled()) {
			logger.info("Sending Request '{}': Scenario '{}', UserId #{}", Array(request.name, context.scenarioName, context.userId))
		}

		feeder.map {
			feeder => context.setAttributes(feeder.next)
		}

		HttpRequestAction.CLIENT.executeRequest(request.getRequest(context), new GatlingAsyncHandler(context, captures, next, request.name, groups))
	}
}
