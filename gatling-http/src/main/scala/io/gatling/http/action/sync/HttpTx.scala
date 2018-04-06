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

package io.gatling.http.action.sync

import io.gatling.commons.stats.OK
import io.gatling.commons.util.ClockSingleton._
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen
import io.gatling.http.cache.{ ContentCacheEntry, HttpCaches }
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.client.{ HttpClient, Request }
import io.gatling.http.engine.{ GatlingHttpListener, HttpEngine }
import io.gatling.http.fetch.RegularResourceFetched
import io.gatling.http.request.HttpRequest
import io.gatling.http.response._

import akka.actor.{ ActorRef, ActorRefFactory, Props }
import com.typesafe.scalalogging.StrictLogging

object HttpTx extends NameGen with StrictLogging {

  def silent(request: HttpRequest, root: Boolean): Boolean = {

    val requestPart = request.config.httpComponents.httpProtocol.requestPart

    def silentBecauseProtocolSilentURI: Boolean = requestPart.silentURI match {
      case Some(silentUri) => silentUri.matcher(request.clientRequest.getUri.toUrl).matches
      case None            => false
    }

    def silentBecauseProtocolSilentResources = !root && requestPart.silentResources

    request.config.silent match {
      case None         => silentBecauseProtocolSilentURI || silentBecauseProtocolSilentResources
      case Some(silent) => silent
    }
  }

  private def startWithCache(origTx: HttpTx, actorRefFactory: ActorRefFactory, httpEngine: HttpEngine, httpCaches: HttpCaches)(f: HttpTx => Unit): Unit = {
    val tx = httpCaches.applyPermanentRedirect(origTx)
    val clientRequest = tx.request.clientRequest
    val uri = clientRequest.getUri

    httpCaches.contentCacheEntry(tx.session, clientRequest) match {

      case None | Some(ContentCacheEntry(None, _, _)) =>
        f(tx)

      case Some(ContentCacheEntry(Some(expire), _, _)) if unpreciseNowMillis > expire =>
        val newTx = tx.copy(session = httpCaches.clearContentCache(tx.session, clientRequest))
        f(newTx)

      case _ =>
        httpEngine.resourceFetcherActorForCachedPage(uri, tx) match {
          case Some(resourceFetcherActor) =>
            logger.info(s"Fetching resources of cached page request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenario}, userId=${tx.session.userId}")
            actorRefFactory.actorOf(Props(resourceFetcherActor()), genName("resourceFetcher"))

          case _ =>
            logger.info(s"Skipping cached request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenario}, userId=${tx.session.userId}")
            tx.resourceTx match {
              case Some(ResourceTx(fetcher, _)) => fetcher ! RegularResourceFetched(uri, OK, Session.Identity, tx.silent)
              case _                            => tx.next ! tx.session
            }
        }
    }
  }

  private def executeRequest(client: HttpClient, ahcRequest: Request, clientId: Long, shared: Boolean, listener: GatlingHttpListener): Unit =
    if (!client.isClosed) {
      listener.start()
      client.sendRequest(ahcRequest, clientId, shared, listener)
    }

  def start(origTx: HttpTx)(implicit actorRefFactory: ActorRefFactory): Unit = {

    import origTx.request.config.httpComponents._

    startWithCache(origTx, actorRefFactory, httpEngine, httpCaches) { tx =>

      logger.debug(s"Sending request=${tx.request.requestName} uri=${tx.request.clientRequest.getUri}: scenario=${tx.session.scenario}, userId=${tx.session.userId}")

      val client = httpEngine.httpClient
      val ahcRequest = tx.request.clientRequest
      val clientId = tx.session.userId
      val shared = httpProtocol.enginePart.shareConnections
      val handler = new GatlingHttpListener(tx, responseProcessor)

      if (tx.request.config.throttled)
        origTx.request.config.coreComponents.throttler.throttle(tx.session.scenario, () => executeRequest(client, ahcRequest, clientId, shared, handler))
      else
        executeRequest(client, ahcRequest, clientId, shared, handler)
    }
  }
}

case class ResourceTx(fetcher: ActorRef, uri: Uri)

case class HttpTx(
    session:                Session,
    request:                HttpRequest,
    responseBuilderFactory: ResponseBuilderFactory,
    next:                   Action,
    resourceTx:             Option[ResourceTx]     = None,
    redirectCount:          Int                    = 0,
    update:                 Session => Session     = Session.Identity
) {
  lazy val silent: Boolean = HttpTx.silent(request, resourceTx.isEmpty)

  lazy val fullRequestName: String =
    if (redirectCount > 0)
      s"${request.requestName} Redirect $redirectCount"
    else
      request.requestName
}
