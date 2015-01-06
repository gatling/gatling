/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.action

import com.typesafe.scalalogging.StrictLogging

import akka.actor.{ ActorRef, ActorContext }
import akka.actor.ActorDSL.actor
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.result.message.OK
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation._
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.cache.{ PermanentRedirect, CacheHandling }
import io.gatling.http.fetch.{ RegularResourceFetched, ResourceFetcher }
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.response._

object HttpRequestAction extends DataWriterClient with AkkaDefaults with StrictLogging {

  def startHttpTransaction(origTx: HttpTx, httpEngine: HttpEngine = HttpEngine.instance)(implicit ctx: ActorContext): Unit = {

      def startHttpTransaction(tx: HttpTx): Unit = {
        logger.info(s"Sending request=${tx.request.requestName} uri=${tx.request.ahcRequest.getUri}: scenario=${tx.session.scenarioName}, userId=${tx.session.userId}")
        httpEngine.startHttpTransaction(tx)
      }

    val tx = PermanentRedirect.applyPermanentRedirect(origTx)
    val uri = tx.request.ahcRequest.getUri
    val protocol = tx.request.config.protocol

    CacheHandling.getExpire(protocol, tx.session, uri) match {

      case None =>
        startHttpTransaction(tx)

      case Some(expire) if nowMillis > expire =>
        val newTx = tx.copy(session = CacheHandling.clearExpire(tx.session, uri))
        startHttpTransaction(newTx)

      case _ =>
        ResourceFetcher.resourceFetcherForCachedPage(uri, tx) match {
          case Some(resourceFetcher) =>
            logger.info(s"Fetching resources of cached page request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenarioName}, userId=${tx.session.userId}")
            actor(ctx, actorName("resourceFetcher"))(resourceFetcher())

          case None =>
            logger.info(s"Skipping cached request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenarioName}, userId=${tx.session.userId}")
            if (tx.primary)
              tx.next ! tx.session
            else
              tx.next ! RegularResourceFetched(uri, OK, Session.Identity, tx.silent)
        }
    }
  }
}

/**
 * This is an action that sends HTTP requests
 *
 * @constructor constructs an HttpRequestAction
 * @param httpRequestDef the request definition
 * @param next the next action that will be executed after the request
 */
class HttpRequestAction(httpRequestDef: HttpRequestDef, val next: ActorRef) extends RequestAction {

  import httpRequestDef._

  val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
    config.checks,
    config.responseTransformer,
    config.discardResponseChunks,
    config.protocol.responsePart.inferHtmlResources)
  val requestName = httpRequestDef.requestName

  def sendRequest(requestName: String, session: Session): Validation[Unit] =
    httpRequestDef.build(requestName, session).map { httpRequest =>

      val tx = HttpTx(
        session,
        httpRequest,
        responseBuilderFactory,
        next: ActorRef)

      HttpRequestAction.startHttpTransaction(tx)
    }
}
