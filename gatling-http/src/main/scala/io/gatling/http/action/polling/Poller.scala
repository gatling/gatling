/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.polling

import java.util.concurrent.{ ScheduledFuture, TimeUnit }

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.http.cache.HttpCaches
import io.gatling.http.engine.response.{ ResponseProcessor, RootSessionProcessor }
import io.gatling.http.engine.tx.{ HttpTx, HttpTxExecutor }
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.response.HttpResult

import com.typesafe.scalalogging.StrictLogging

private[polling] final class Poller(
    pollerName: String,
    period: FiniteDuration,
    requestDef: HttpRequestDef,
    httpTxExecutor: HttpTxExecutor,
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol,
    statsEngine: StatsEngine
) extends StrictLogging {
  private var session: Session = _
  private var timer: ScheduledFuture[_] = _

  def start(session: Session): Unit = {
    this.session = session
    timer = session.eventLoop.scheduleAtFixedRate(() => poll(), 0, period.toMillis, TimeUnit.MILLISECONDS)
  }

  private def poll(): Unit =
    requestDef.build(session) match {
      case Success(httpRequest) =>
        httpTxExecutor.execute(
          HttpTx(session, httpRequest, next = null, resourceTx = None, redirectCount = 0),
          tx =>
            new ResponseProcessor() {
              override def onComplete(result: HttpResult): Unit =
                // FIXME not great, should we cancel timer on request failure?
                session = resourceFetched(tx, result)
            }
        )
      case Failure(requestNameError) =>
        val errorMessage = s"Failed to build polling request $pollerName: $requestNameError"
        logger.error(errorMessage)
        statsEngine.logRequestCrash(session.scenario, session.groups, pollerName, errorMessage)
        timer.cancel(false)
        session = session.markAsFailed
    }

  private def resourceFetched(tx: HttpTx, result: HttpResult): Session =
    new PollerResponseProcessor(
      tx.copy(session = session),
      sessionProcessor = new RootSessionProcessor(
        tx.silent,
        tx.request.clientRequest,
        tx.request.requestConfig.checks,
        httpCaches,
        httpProtocol
      ),
      statsProcessor = httpTxExecutor.statsProcessor(tx),
      tx.request.requestConfig.defaultCharset
    ).onComplete(result)

  def stop(next: Action, session: Session): Unit = {
    timer.cancel(false)
    // FIXME all state change is lost
    next ! session.remove(pollerName)
  }
}
