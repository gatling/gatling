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

import com.typesafe.scalalogging.slf4j.StrictLogging

import akka.actor.{ ActorRef, ActorContext }
import akka.actor.ActorDSL.actor
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.cache.CacheHandling
import io.gatling.http.fetch.ResourceFetcher
import io.gatling.http.request.HttpRequest
import io.gatling.http.response.ResponseBuilder
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import collection.convert.WrapAsScala.mapAsScalaConcurrentMap
import com.ning.http.client.{ RequestBuilder, Request }

private[action] trait HttpRequestActionExecutor {
  def startHttpTransaction(tx: HttpTx)(implicit ctx: ActorContext)

  def httpTransactionRedirect(originalURI: URI, tx: HttpTx)(implicit ctx: ActorContext)

  private[action] def cleanRedirectCache()
}

object HttpRequestAction extends StrictLogging {

  private[action] var instance = new HttpRequestActionExecutor {
    private val redirectMap = mapAsScalaConcurrentMap(new ConcurrentHashMap[URI, URI]())

    def startHttpTransaction(origTx: HttpTx)(implicit ctx: ActorContext) {

        def startHttpTransaction(tx: HttpTx) {
          logger.info(s"Sending request=${tx.requestName} uri=${tx.request.getURI}: scenario=${tx.session.scenarioName}, userId=${tx.session.userId}")
          HttpEngine.instance.startHttpTransaction(tx)
        }

        def skipCached(tx: HttpTx) {
          logger.info(s"Skipping cached request=${tx.requestName} uri=${tx.request.getURI}: scenario=${tx.session.scenarioName}, userId=${tx.session.userId}")
          tx.next ! tx.session
        }

      val uri = origTx.request.getURI

      val tx = permanentRedirect(uri) match {
        case Some(Pair(targetUri, redirectCount)) => {
          redirectTransaction(origTx, targetUri, redirectCount)
        }

        case None => origTx
      }

      CacheHandling.getExpire(tx.protocol, tx.session, uri) match {

        case None                               => startHttpTransaction(tx)

        case Some(expire) if nowMillis > expire => startHttpTransaction(tx.copy(session = CacheHandling.clearExpire(tx.session, uri)))

        case _ if tx.protocol.responsePart.fetchHtmlResources =>
          val explicitResources = HttpRequest.buildNamedRequests(tx.explicitResources, tx.session)

          ResourceFetcher.fromCache(tx.request.getURI, tx, explicitResources) match {
            case Some(resourceFetcher) =>
              logger.info(s"Fetching resources of cached page request=${tx.requestName} uri=${tx.request.getURI}: scenario=${tx.session.scenarioName}, userId=${tx.session.userId}")
              actor(resourceFetcher())

            case None => skipCached(tx)
          }

        case _ => skipCached(tx)
      }
    }

    private def permanentRedirect(uri: URI): Option[Pair[URI, Int]] = {
        def permanentRedirect1(from: URI, redirectCount: Int): Option[Pair[URI, Int]] = {
          redirectMap.get(from) match {
            case Some(toUri) => {
              permanentRedirect1(toUri, redirectCount + 1)
            }

            case None => {
              redirectCount match {
                case 0 => None
                case _ => Some(Pair(from, redirectCount))
              }
            }
          }
        }

      permanentRedirect1(uri, 0)
    }

    private def redirectTransaction(origTx: HttpTx, uri: URI, additionalRedirects: Int): HttpTx = {
      val newRequest = redirectRequest(origTx.request, uri)
      origTx.copy(request = newRequest, redirectCount = origTx.redirectCount + additionalRedirects)
    }

    private def redirectRequest(request: Request, toUri: URI): Request = {
      val requestBuilder = new RequestBuilder(request)
      requestBuilder.setURI(toUri)

      requestBuilder.build()
    }

    def httpTransactionRedirect(originalURI: URI, tx: HttpTx)(implicit ctx: ActorContext) {
      memoizeRedirect(originalURI, tx.request.getURI)
      startHttpTransaction(tx)
    }

    private def memoizeRedirect(from: URI, to: URI) {
      redirectMap += from -> to
    }

    override private[action] def cleanRedirectCache() {
      redirectMap.clear()
    }
  }

  def startHttpTransaction(tx: HttpTx)(implicit ctx: ActorContext) {
    instance.startHttpTransaction(tx)
  }

  def httpTransactionRedirect(originalURI: URI, tx: HttpTx)(implicit ctx: ActorContext) {
    instance.httpTransactionRedirect(originalURI, tx)
  }
}

/**
 * This is an action that sends HTTP requests
 *
 * @constructor constructs an HttpRequestAction
 * @param httpRequest the request
 * @param next the next action that will be executed after the request
 */
class HttpRequestAction(httpRequest: HttpRequest, val next: ActorRef) extends RequestAction {

  import httpRequest._

  val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(checks, responseTransformer, protocol)
  val requestName = httpRequest.requestName

  def sendRequest(requestName: String, session: Session) =
    for {
      ahcRequest <- ahcRequest(session)
      tx = HttpTx(session, ahcRequest, requestName, checks, responseBuilderFactory, protocol, next, followRedirect, maxRedirects, throttled, silent, explicitResources, extraInfoExtractor)
    } yield HttpRequestAction.startHttpTransaction(tx)
}
