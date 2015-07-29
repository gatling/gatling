/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.ahc

import io.gatling.core.akka.ActorNames
import io.gatling.core.session.Session
import io.gatling.core.stats.message.OK
import io.gatling.core.util.TimeHelper._
import io.gatling.http.cache.ContentCacheEntry
import io.gatling.http.fetch.RegularResourceFetched
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.HttpRequest
import io.gatling.http.response._

import akka.actor.{ Props, ActorContext, ActorRef }
import com.typesafe.scalalogging.StrictLogging

object HttpTx extends ActorNames with StrictLogging {

  def silent(request: HttpRequest, root: Boolean): Boolean = {

    val requestPart = request.config.httpComponents.httpProtocol.requestPart

      def silentBecauseProtocolSilentResources = !root && requestPart.silentResources

      def silentBecauseProtocolSilentURI: Option[Boolean] = requestPart.silentURI
        .map(_.matcher(request.ahcRequest.getUrl).matches)

    request.config.silent.orElse(silentBecauseProtocolSilentURI).getOrElse(silentBecauseProtocolSilentResources)
  }

  private def startWithCache(origTx: HttpTx, ctx: ActorContext, httpComponents: HttpComponents)(f: HttpTx => Unit): Unit = {
    import httpComponents._
    val tx = httpComponents.httpCaches.applyPermanentRedirect(origTx)
    val ahcRequest = tx.request.ahcRequest
    val uri = ahcRequest.getUri

    httpCaches.contentCacheEntry(tx.session, ahcRequest) match {

      case None =>
        f(tx)

      case Some(ContentCacheEntry(Some(expire), _, _)) if nowMillis > expire =>
        val newTx = tx.copy(session = httpCaches.clearContentCache(tx.session, ahcRequest))
        f(newTx)

      case _ =>
        httpEngine.resourceFetcherActorForCachedPage(uri, tx) match {
          case Some(resourceFetcherActor) =>
            logger.info(s"Fetching resources of cached page request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenario}, userId=${tx.session.userId}")
            ctx.actorOf(Props(resourceFetcherActor()), actorName("resourceFetcher"))

          case None =>
            logger.info(s"Skipping cached request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenario}, userId=${tx.session.userId}")
            if (tx.root)
              tx.next ! tx.session
            else
              tx.next ! RegularResourceFetched(uri, OK, Session.Identity, tx.silent)
        }
    }
  }

  def start(origTx: HttpTx, httpComponents: HttpComponents)(implicit ctx: ActorContext): Unit =
    startWithCache(origTx, ctx, httpComponents) { tx =>

      val httpEngine = httpComponents.httpEngine
      logger.info(s"Sending request=${tx.request.requestName} uri=${tx.request.ahcRequest.getUri}: scenario=${tx.session.scenario}, userId=${tx.session.userId}")

      val requestConfig = tx.request.config

      httpEngine.httpClient(tx.session, requestConfig.httpComponents.httpProtocol) match {
        case (newSession, client) =>
          val newTx = tx.copy(session = newSession)
          val ahcRequest = newTx.request.ahcRequest
          val handler = new AsyncHandler(newTx, httpEngine)

          if (requestConfig.throttled)
            httpEngine.coreComponents.throttler.throttle(tx.session.scenario, () => client.executeRequest(ahcRequest, handler))
          else
            client.executeRequest(ahcRequest, handler)

        case _ => // client has been shutdown, ignore
      }
    }
}

case class HttpTx(
    session:                Session,
    request:                HttpRequest,
    responseBuilderFactory: ResponseBuilderFactory,
    next:                   ActorRef,
    root:                   Boolean                = true,
    redirectCount:          Int                    = 0,
    update:                 Session => Session     = Session.Identity
) {

  val silent: Boolean = HttpTx.silent(request, root)
}
