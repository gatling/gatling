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

import com.excilys.ebi.gatling.core.action.{ Action, RequestAction }
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.http.ahc.CustomAsyncHandler
import com.excilys.ebi.gatling.http.processor.check.HttpStatusCheck
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.resource.HttpClientResource
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.ning.http.client.AsyncHttpClient
import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }
import com.ning.http.client.AsyncHttpClientConfig
import org.joda.time.DateTime

object HttpRequestAction {
	val CLIENT: AsyncHttpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setCompressionEnabled(true).build())
	ResourceRegistry.register(new HttpClientResource(CLIENT))
}
class HttpRequestAction(next: Action, request: HttpRequest, givenProcessorBuilders: Option[List[HttpProcessorBuilder]], groups: List[String], feeder: Option[Feeder])
		extends RequestAction(next, request, givenProcessorBuilders, groups, feeder) {

	val processors: MultiMap[HttpPhase, HttpProcessor] = new HashMap[HttpPhase, MSet[HttpProcessor]] with MultiMap[HttpPhase, HttpProcessor]

	givenProcessorBuilders match {
		case Some(list) => {
			for (processorBuilder <- list) {
				val processor = processorBuilder.build
				processors.addBinding(processor.getHttpPhase, processor)
				logger.debug("  -- Adding {} to {} Phase", processor, processor.getHttpPhase)
			}
		}
		case None => {}
	}

	// Adds default checks (they won't be added if overridden by user)
	processors.addBinding(StatusReceived, new HttpStatusCheck((200 to 210).mkString(":"), EMPTY))

	def execute(context: Context) = {
		val objects = new Array[java.lang.Object](3)
		objects(0) = request.name
		objects(1) = context.getScenarioName
		objects(2) = context.getUserId.toString
		logger.info("Sending Request '{}': Scenario '{}', UserId #{}", objects)

		feeder.map {
			feeder =>
				context.setAttributes(feeder.next)
		}

		HttpRequestAction.CLIENT.executeRequest(request.getRequest(context), new CustomAsyncHandler(context, processors, next, System.nanoTime, DateTime.now(), request.getName, groups))
	}
}
