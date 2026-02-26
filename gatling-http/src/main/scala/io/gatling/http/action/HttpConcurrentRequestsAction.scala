/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.action

import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.action.{ Action, ExitableAction }
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.gatling.http.engine.tx.HttpTxExecutor
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.request.builder.HttpRequestBuilder

final class HttpConcurrentRequestsActionBuilder(requests: List[HttpRequestBuilder]) extends HttpActionBuilder {
  require(requests.nonEmpty, "httpConcurrentRequests requests list can't be empty")
  require(!requests.contains(null), "httpConcurrentRequests can't contain null elements. Forward reference issue?")
  require(!requests.exists(_.httpAttributes.explicitResources.nonEmpty), "httpConcurrentRequests requests can't have resources")

  override def build(ctx: ScenarioContext, next: Action): Action = {
    val httpComponents = lookUpHttpComponents(ctx.protocolComponentsRegistry)
    val httpRequestDefs =
      requests.map(_.build(httpComponents.httpCaches, httpComponents.httpProtocol, ctx.throttled, ctx.coreComponents.configuration))

    new HttpConcurrentRequestsAction(
      httpRequestDefs,
      httpComponents.httpTxExecutor,
      httpComponents.httpProtocol,
      ctx.coreComponents,
      next
    )
  }
}

/**
 * This is an action that sends concurrent HTTP requests
 */
final class HttpConcurrentRequestsAction(
    httpRequestDefs: List[HttpRequestDef],
    httpTxExecutor: HttpTxExecutor,
    httpProtocol: HttpProtocol,
    coreComponents: CoreComponents,
    override val next: Action
) extends ExitableAction
    with NameGen {

  override def clock: Clock = coreComponents.clock

  override val name: String = genName("httpConcurrentRequests")

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  override def execute(session: Session): Unit = {
    val httpRequestsV = httpRequestDefs.map { httpRequestDef =>
      val httpRequestV = safely()(httpRequestDef.build(session))
      httpRequestV -> httpRequestDef
    }

    val (failedHttpRequestDefs, successfulHttpRequests) = httpRequestsV.partitionMap {
      case (Success(httpRequest), _)        => Right(httpRequest)
      case (Failure(error), httpRequestDef) => Left((error, httpRequestDef))
    }

    failedHttpRequestDefs.headOption match {
      case Some((error, failedHttpRequestDef)) => logCrash(session, error, failedHttpRequestDef)
      case _ =>
        val resourceAggregator =
          httpTxExecutor.resourceFetcher.newConcurrentRequestsAggregator(successfulHttpRequests, httpProtocol = httpProtocol, next = next)
        resourceAggregator.start(session)
    }
  }

  private def logCrash(session: Session, error: String, failedHttpRequestDef: HttpRequestDef): Unit =
    failedHttpRequestDef.requestName(session) match {
      case Success(requestNameValue) =>
        logger.error(s"Failed to build request $requestNameValue: $error")
        statsEngine.logRequestCrash(session.scenario, session.groups, requestNameValue, error)
      case Failure(requestNameError) =>
        val errorMessage = s"Failed to build request name: $requestNameError"
        logger.error(errorMessage)
    }
}
