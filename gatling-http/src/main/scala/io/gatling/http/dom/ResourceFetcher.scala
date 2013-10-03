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
package io.gatling.http.dom

import com.ning.http.client.Request
import io.gatling.core.akka.BaseActor
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.session.Expression
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Success, SuccessWrapper }
import io.gatling.http.action.HttpRequestAction
import io.gatling.http.ahc.HttpTx
import io.gatling.http.request.builder.HttpRequestBaseBuilder
import io.gatling.http.action.HttpRequestActionBuilder
import io.gatling.http.response.ResponseBuilder

case class ResourceFetched(request: Request, status: Status)

object ResourceFetcher {

	val resourceChecks = List(HttpRequestActionBuilder.defaultHttpCheck)
}

class ResourceFetcher(urls: Seq[String], tx: HttpTx) extends BaseActor {

	def handleResource(request: Request) {
		logger.debug(s"Fetching ressource ${request.getUrl}")
		val resourceTx = tx.copy(
			request = request,
			requestName = request.getUrl,
			checks = ResourceFetcher.resourceChecks,
			responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(ResourceFetcher.resourceChecks, None, tx.protocol),
			next = self,
			resourceFetching = true)

		HttpRequestAction.handleHttpTransaction(resourceTx)
	}

	var globalStatus: Status = OK
	var pending = urls.size
	var pendingRequestsByHost: Map[String, List[Request]] =
		urls.map { url =>
			val urlExpression: Expression[String] = _ => url.success
			val requestBuilder = HttpRequestBaseBuilder.http(urlExpression).get(urlExpression)
			requestBuilder.build(tx.session, tx.protocol)
		}
			.collect { case Success(request) => request }
			.groupBy(_.getURI.getHost)
			.mapValues { requests =>
				val (immediate, pending) = requests.splitAt(tx.protocol.maxConnectionsPerHost)
				immediate.foreach(handleResource)
				pending.toList
			}.filter(!_._2.isEmpty)

	val start = nowMillis

	def receive: Receive = {
		case ResourceFetched(request, status) =>
			logger.debug(s"Resource ${request.getUrl} was fetched")
			pending -= 1
			if (status == KO)
				globalStatus = KO

			if (pending == 0) {
				logger.debug("All resources were fetched")
				val newSession = (if (globalStatus == KO) tx.session.markAsFailed else tx.session).logGroupRequest(nowMillis - start, status)
				tx.next ! newSession
				context.stop(self)

			} else {
				// try to find a pending request for the same host
				val host = request.getURI.getHost
				pendingRequestsByHost.get(host) match {
					case Some(head :: tail) =>
						handleResource(head)
						pendingRequestsByHost =
							if (tail.isEmpty)
								pendingRequestsByHost - host
							else
								pendingRequestsByHost + (host -> tail)
					case _ =>
						// nothing to send for this host
						logger.debug(s"No resource left for host ${host}")
				}
			}
	}
}
