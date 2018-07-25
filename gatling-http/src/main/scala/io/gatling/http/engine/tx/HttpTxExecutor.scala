/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.engine.tx

import io.gatling.core.CoreComponents
import io.gatling.core.util.NameGen
import io.gatling.http.cache.{ ContentCacheEntry, HttpCaches }
import io.gatling.http.engine.{ GatlingHttpListener, HttpEngine }
import io.gatling.http.engine.response._
import io.gatling.http.fetch.ResourceFetcher
import io.gatling.http.protocol.HttpProtocol

import com.typesafe.scalalogging.StrictLogging

class HttpTxExecutor(
    coreComponents:        CoreComponents,
    httpEngine:            HttpEngine,
    httpCaches:            HttpCaches,
    defaultStatsProcessor: DefaultStatsProcessor,
    httpProtocol:          HttpProtocol
) extends NameGen with StrictLogging {

  import coreComponents._

  private val resourceFetcher = new ResourceFetcher(coreComponents, httpCaches, httpProtocol, this)

  private def executeWithCache(origTx: HttpTx)(f: HttpTx => Unit): Unit = {
    val tx = httpCaches.applyPermanentRedirect(origTx)
    val clientRequest = tx.request.clientRequest
    val uri = clientRequest.getUri

    httpCaches.contentCacheEntry(tx.session, clientRequest) match {
      case None | Some(ContentCacheEntry(None, _, _)) =>
        f(tx)

      case Some(ContentCacheEntry(Some(expire), _, _)) if clock.nowMillis > expire =>
        val newTx = tx.copy(session = httpCaches.clearContentCache(tx.session, clientRequest))
        f(newTx)

      case _ =>
        resourceFetcher.newResourceAggregatorForCachedPage(uri, tx) match {
          case Some(aggregator) =>
            logger.info(s"Fetching resources of cached page request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenario}, userId=${tx.session.userId}")
            aggregator.start()

          case _ =>
            logger.info(s"Skipping cached request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenario}, userId=${tx.session.userId}")
            tx.resourceTx match {
              case Some(ResourceTx(aggregator, _)) => aggregator.onCachedResource(uri, tx)
              case _                               => tx.next ! tx.session
            }
        }
    }
  }

  def execute(origTx: HttpTx): Unit =
    execute(origTx, (tx: HttpTx) => {
      tx.resourceTx match {
        case Some(resourceTx) => newResourceResponseProcessor(tx, resourceTx)
        case _                => newRootResponseProcessor(tx)
      }
    })

  def execute(origTx: HttpTx, responseProcessorFactory: HttpTx => ResponseProcessor): Unit =
    executeWithCache(origTx) { tx =>
      logger.debug(s"Sending request=${tx.request.requestName} uri=${tx.request.clientRequest.getUri}: scenario=${tx.session.scenario}, userId=${tx.session.userId}")

      val ahcRequest = tx.request.clientRequest
      val clientId = tx.session.userId
      val shared = tx.request.requestConfig.httpProtocol.enginePart.shareConnections
      val listener = new GatlingHttpListener(tx, coreComponents, responseProcessorFactory(tx))

      if (tx.request.requestConfig.throttled)
        throttler.throttle(tx.session.scenario, () => httpEngine.executeRequest(ahcRequest, clientId, shared, listener))
      else
        httpEngine.executeRequest(ahcRequest, clientId, shared, listener)
    }

  private def newRootResponseProcessor(tx: HttpTx): ResponseProcessor =
    new DefaultResponseProcessor(
      tx,
      sessionProcessor =
        new RootSessionProcessor(
          !tx.silent,
          tx.request.clientRequest,
          tx.request.requestConfig.checks,
          httpCaches,
          httpProtocol,
          clock
        ),
      statsProcessor = statsProcessor(tx),
      nextExecutor = new RootNextExecutor(tx, clock, resourceFetcher, this),
      configuration.core.charset
    )

  private def newResourceResponseProcessor(tx: HttpTx, resourceTx: ResourceTx): ResponseProcessor =
    new DefaultResponseProcessor(
      tx = tx.copy(session = resourceTx.aggregator.currentSession),
      sessionProcessor = new ResourceSessionProcessor(
        !tx.silent,
        tx.request.clientRequest,
        tx.request.requestConfig.checks,
        httpCaches,
        httpProtocol,
        clock
      ),
      statsProcessor = statsProcessor(tx),
      nextExecutor = new ResourceNextExecutor(tx, resourceTx),
      configuration.core.charset
    )

  def statsProcessor(tx: HttpTx): StatsProcessor =
    if (tx.silent) NoopStatsProcessor else defaultStatsProcessor
}
