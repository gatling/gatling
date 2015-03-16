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
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message.OK
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation._
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.fetch.RegularResourceFetched
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.response._

object HttpRequestAction extends AkkaDefaults with StrictLogging {

  // FIXME Move to HttpEngine?
  def startHttpTransaction(origTx: HttpTx)(implicit ctx: ActorContext, httpEngine: HttpEngine): Unit = {

    val tx = httpEngine.httpCaches.applyPermanentRedirect(origTx)
    val uri = tx.request.ahcRequest.getUri
    val method = tx.request.ahcRequest.getMethod

    httpEngine.httpCaches.getExpires(tx.session, uri, method) match {

      case None =>
        httpEngine.startHttpTransaction(tx)

      case Some(expire) if nowMillis > expire =>
        val newTx = tx.copy(session = httpEngine.httpCaches.clearExpires(tx.session, uri, method))
        httpEngine.startHttpTransaction(newTx)

      case _ =>
        httpEngine.resourceFetcherActorForCachedPage(uri, tx) match {
          case Some(resourceFetcherActor) =>
            logger.info(s"Fetching resources of cached page request=${tx.request.requestName} uri=$uri: scenario=${tx.session.scenarioName}, userId=${tx.session.userId}")
            actor(ctx, actorName("resourceFetcher"))(resourceFetcherActor())

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
 * @param dataWriters the DataWriters
 * @param next the next action that will be executed after the request
 */
class HttpRequestAction(httpRequestDef: HttpRequestDef, dataWriters: DataWriters, val next: ActorRef)(implicit configuration: GatlingConfiguration, httpEngine: HttpEngine)
    extends RequestAction(dataWriters) {

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
