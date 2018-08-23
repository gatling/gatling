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

package io.gatling.http.fetch

import scala.collection.mutable

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.http.cache.{ ContentCacheEntry, HttpCaches }
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.engine.tx.{ HttpTx, HttpTxExecutor, ResourceTx }
import io.gatling.http.request.HttpRequest
import io.gatling.http.response.ResponseBuilder

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.HttpResponseStatus

trait ResourceAggregator {

  def currentSession: Session

  def start(session: Session): Unit

  def onRegularResourceFetched(uri: Uri, status: Status, session: Session, silent: Boolean): Unit

  def onCssResourceFetched(uri: Uri, status: Status, session: Session, silent: Boolean, responseStatus: HttpResponseStatus, lastModifiedOrEtag: Option[String], content: String): Unit

  def onRedirect(originalTx: HttpTx, redirectTx: HttpTx): Unit

  def onCachedResource(uri: Uri, tx: HttpTx): Unit
}

class DefaultResourceAggregator(
    rootTx:           HttpTx,
    initialResources: Seq[HttpRequest],
    httpCaches:       HttpCaches,
    resourceFetcher:  ResourceFetcher,
    httpTxExecutor:   HttpTxExecutor,
    clock:            Clock,
    configuration:    GatlingConfiguration
) extends ResourceAggregator with StrictLogging {

  // immutable state
  private val throttled = rootTx.request.requestConfig.throttled
  private val httpProtocol = rootTx.request.requestConfig.httpProtocol

  // mutable state
  private var session: Session = _
  private val alreadySeen = mutable.Set.empty[Uri]
  private val bufferedResourcesByHost = mutable.HashMap.empty[String, List[HttpRequest]].withDefaultValue(Nil)
  private val availableTokensByHost = mutable.HashMap.empty[String, Int].withDefaultValue(httpProtocol.enginePart.maxConnectionsPerHost)
  private var pendingResourcesCount = 0
  private var globalStatus: Status = OK
  private val startTimestamp = clock.nowMillis

  // start fetching

  override def currentSession: Session = session

  override def start(session: Session): Unit = {
    this.session = session
    fetchOrBufferResources(initialResources)
  }

  private def fetchResource(resource: HttpRequest): Unit = {
    logger.debug(s"Fetching resource ${resource.clientRequest.getUri}")

    val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
      resource.requestConfig.checks,
      httpProtocol.responsePart.discardResponseChunks,
      httpProtocol.responsePart.inferHtmlResources,
      clock,
      configuration
    )

    val resourceTx = rootTx.copy(
      session = this.session,
      request = resource,
      responseBuilderFactory = responseBuilderFactory,
      resourceTx = Some(ResourceTx(this, resource.clientRequest.getUri)),
      redirectCount = 0
    )

    httpTxExecutor.execute(resourceTx)
  }

  private def handleCachedResource(resource: HttpRequest): Unit = {

    val uri = resource.clientRequest.getUri

    logger.info(s"Fetching resource $uri from cache")

    val silent = resource.isSilent(root = false)

    // FIXME check if it's a css this way or use the Content-Type?
    if (httpCaches.isCssCached(uri)) {
      onCssResourceFetched(uri, OK, this.session, silent, HttpResponseStatus.NOT_MODIFIED, None, "")
    } else {
      onRegularResourceFetched(uri, OK, this.session, silent)
    }
  }

  private def fetchOrBufferResources(resources: Iterable[HttpRequest]): Unit = {

    def fetchResources(host: String, resources: Iterable[HttpRequest]): Unit = {
      resources.foreach(fetchResource)
      availableTokensByHost += host -> (availableTokensByHost(host) - resources.size)
    }

    def bufferResources(host: String, resources: Iterable[HttpRequest]): Unit =
      bufferedResourcesByHost += host -> (bufferedResourcesByHost(host) ::: resources.toList)

    alreadySeen ++= resources.map(_.clientRequest.getUri)
    pendingResourcesCount += resources.size

    val (cached, nonCached) = resources.partition { resource =>
      val request = resource.clientRequest
      httpCaches.contentCacheEntry(session, request) match {
        case None | Some(ContentCacheEntry(None, _, _)) => false
        case Some(ContentCacheEntry(Some(expire), _, _)) if clock.nowMillis > expire =>
          // beware, side effecting
          session = httpCaches.clearContentCache(session, request)
          false
        case _ => true
      }
    }

    cached.foreach(handleCachedResource)

    nonCached
      .groupBy(_.clientRequest.getUri.getHost)
      .foreach {
        case (host, res) =>
          val availableTokens = availableTokensByHost(host)
          val (immediate, buffered) = res.splitAt(availableTokens)
          fetchResources(host, immediate)
          bufferResources(host, buffered)
      }
  }

  private def done(): Unit = {
    logger.debug("All resources were fetched")
    val newSession =
      if (rootTx.silent) {
        session
      } else {
        session.logGroupRequest(startTimestamp, clock.nowMillis, globalStatus)
      }

    rootTx.next ! newSession
  }

  private def resourceFetched(uri: Uri, status: Status, silent: Boolean): Unit = {

    def releaseToken(host: String): Unit =
      bufferedResourcesByHost.get(host) match {
        case Some(Nil) | None =>
          // nothing to send for this host for now
          availableTokensByHost += host -> (availableTokensByHost(host) + 1)

        case Some(request :: tail) =>
          bufferedResourcesByHost += host -> tail
          val clientRequest = request.clientRequest
          httpCaches.contentCacheEntry(session, clientRequest) match {
            case None =>
              // recycle token, fetch a buffered resource
              fetchResource(request)

            case Some(ContentCacheEntry(Some(expire), _, _)) if clock.nowMillis > expire =>
              // expire reached
              session = httpCaches.clearContentCache(session, clientRequest)
              fetchResource(request)

            case _ =>
              handleCachedResource(request)
          }
      }

    logger.debug(s"Resource $uri was fetched")
    pendingResourcesCount -= 1

    if (!silent && status == KO)
      globalStatus = KO

    if (pendingResourcesCount == 0)
      done()
    else
      releaseToken(uri.getHost)
  }

  private def cssFetched(uri: Uri, status: Status, responseStatus: HttpResponseStatus, lastModifiedOrEtag: Option[String], content: String): Unit =
    if (status == OK) {
      val cssResources = resourceFetcher.cssFetched(uri, responseStatus, lastModifiedOrEtag, content, session, throttled)
      if (cssResources.nonEmpty) {
        val filtered = cssResources.filterNot(resource => alreadySeen.contains(resource.clientRequest.getUri))
        fetchOrBufferResources(filtered)
      }
    }

  override def onRegularResourceFetched(uri: Uri, status: Status, session: Session, silent: Boolean): Unit = {
    this.session = session
    resourceFetched(uri, status, silent)
  }

  override def onCssResourceFetched(uri: Uri, status: Status, session: Session, silent: Boolean, responseStatus: HttpResponseStatus, lastModifiedOrEtag: Option[String], content: String): Unit = {
    this.session = session
    cssFetched(uri, status, responseStatus, lastModifiedOrEtag, content)
    resourceFetched(uri, status, silent)
  }

  override def onRedirect(originalTx: HttpTx, redirectTx: HttpTx): Unit = {
    this.session = redirectTx.session
    // FIXME
    // free semaphore if different host
    // add to queue if different host
    httpTxExecutor.execute(redirectTx)
  }

  override def onCachedResource(uri: Uri, tx: HttpTx): Unit =
    resourceFetched(uri, OK, tx.silent)
}
