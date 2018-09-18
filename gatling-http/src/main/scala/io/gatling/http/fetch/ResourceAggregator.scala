/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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
import io.gatling.http.protocol.Remote
import io.gatling.http.request.HttpRequest
import io.gatling.http.response.ResponseBuilder

import com.typesafe.scalalogging.StrictLogging
import com.softwaremill.quicklens._
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
  private val bufferedResourcesByHost = mutable.HashMap.empty[Remote, List[HttpRequest]].withDefaultValue(Nil)
  private val maxConnectionsPerHost = httpProtocol.enginePart.maxConnectionsPerHost
  private val availableTokensByHost = mutable.HashMap.empty[Remote, Int].withDefaultValue(maxConnectionsPerHost)
  private var pendingResourcesCount = 0
  private var globalStatus: Status = OK
  private val startTimestamp = clock.nowMillis

  // start fetching

  override def currentSession: Session = session

  override def start(session: Session): Unit = {
    this.session = session
    fetchOrBufferResources(initialResources)
  }

  private def createResourceTx(resource: HttpRequest, isHttp2PriorKnowledge: Option[Boolean]): HttpTx = {
    logger.debug(s"Fetching resource ${resource.clientRequest.getUri}")

    val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
      resource.requestConfig.checks,
      httpProtocol.responsePart.inferHtmlResources,
      clock,
      configuration
    )

    // ALPN is necessary only if HTTP/2 is enabled and if we know that this remote is using HTTP/2 or if we still don't know
    val isAlpnRequired = rootTx.request.clientRequest.isHttp2Enabled && isHttp2PriorKnowledge.forall(_ == true)

    val resourceTx = rootTx.copy(
      session = this.session,
      request = resource.modify(_.clientRequest).using(_.copyWithAlpnRequiredAndPriorKnowledge(isAlpnRequired, isHttp2PriorKnowledge.contains(true))),
      responseBuilderFactory = responseBuilderFactory,
      resourceTx = Some(ResourceTx(this, resource.clientRequest.getUri)),
      redirectCount = 0
    )

    logger.debug(s"Creating resourceTx $resourceTx")
    resourceTx
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

    def fetchAndBufferWithTokens(remote: Remote, resources: Iterable[HttpRequest], isHttp2PriorKnowledge: Option[Boolean]): Unit = {
      val availableTokens = availableTokensByHost(remote)
      val (immediate, buffered) = resources.splitAt(availableTokens)
      fetchHttp1Resources(remote, immediate, isHttp2PriorKnowledge)
      bufferResources(remote, buffered)
    }

    def fetchHttp1Resources(remote: Remote, resources: Iterable[HttpRequest], isHttp2PriorKnowledge: Option[Boolean]): Unit = {
      availableTokensByHost += remote -> (availableTokensByHost(remote) - resources.size)
      resources.foreach(resource => httpTxExecutor.execute(createResourceTx(resource, isHttp2PriorKnowledge)))
    }

    def bufferResources(remote: Remote, resources: Iterable[HttpRequest]): Unit =
      bufferedResourcesByHost += remote -> (bufferedResourcesByHost(remote) ::: resources.toList)

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

    val requestsByRemote = nonCached.groupBy(resource => Remote(resource.clientRequest.getUri))

    if (httpProtocol.requestPart.enableHttp2) {
      requestsByRemote.foreach {
        case (remote, res) =>
          val isHttp2PriorKnowledge = httpCaches.isHttp2PriorKnowledge(session, remote)
          if (isHttp2PriorKnowledge.contains(true)) {
            httpTxExecutor.execute(resources.map(createResourceTx(_, isHttp2PriorKnowledge)))
          } else {
            fetchAndBufferWithTokens(remote, res, isHttp2PriorKnowledge)
          }
      }
    } else {
      requestsByRemote.foreach {
        case (remote, res) =>
          fetchAndBufferWithTokens(remote, res, Some(false))
      }
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

  private def sendBufferedRequest(request: HttpRequest, remote: Remote): Unit = {
    httpCaches.contentCacheEntry(session, request.clientRequest) match {
      case None =>
        // recycle token, fetch a buffered resource
        httpTxExecutor.execute(createResourceTx(request, httpCaches.isHttp2PriorKnowledge(session, remote)))

      case Some(ContentCacheEntry(Some(expire), _, _)) if clock.nowMillis > expire =>
        // expire reached
        session = httpCaches.clearContentCache(session, request.clientRequest)
        httpTxExecutor.execute(createResourceTx(request, httpCaches.isHttp2PriorKnowledge(session, remote)))

      case _ =>
        handleCachedResource(request)
    }
  }

  private def releaseTokenAndContinue(remote: Remote, isHttp2: Boolean): Unit = {
    bufferedResourcesByHost.get(remote) match {
      case Some(Nil) | None =>
        // nothing to send for this remote for now
        val availableToken = availableTokensByHost(remote)
        availableTokensByHost += remote -> (if (availableToken == maxConnectionsPerHost) availableToken else availableToken + 1)

      case Some(requests) =>
        if (isHttp2) {
          bufferedResourcesByHost.remove(remote)
          requests.foreach(sendBufferedRequest(_, remote))
        } else {
          val request :: tail = requests
          bufferedResourcesByHost += remote -> tail
          sendBufferedRequest(request, remote)
        }
    }
  }

  private def resourceFetched(remote: Remote, status: Status, silent: Boolean, isHttp2: Boolean): Unit = {

    pendingResourcesCount -= 1

    if (!silent && status == KO)
      globalStatus = KO

    if (pendingResourcesCount == 0)
      done()
    else
      releaseTokenAndContinue(remote, isHttp2)
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
    logger.debug(s"Resource $uri was fetched")
    this.session = session
    val remote = Remote(uri)
    resourceFetched(remote, status, silent, httpCaches.isHttp2PriorKnowledge(session, remote).contains(true))
  }

  override def onCssResourceFetched(uri: Uri, status: Status, session: Session, silent: Boolean, responseStatus: HttpResponseStatus, lastModifiedOrEtag: Option[String], content: String): Unit = {
    logger.debug(s"Resource $uri was fetched")
    this.session = session
    cssFetched(uri, status, responseStatus, lastModifiedOrEtag, content)
    val remote = Remote(uri)
    resourceFetched(remote, status, silent, httpCaches.isHttp2PriorKnowledge(session, remote).contains(true))
  }

  override def onRedirect(originalTx: HttpTx, redirectTx: HttpTx): Unit = {
    this.session = redirectTx.session
    val originUri = originalTx.request.clientRequest.getUri
    val originRemote = Remote(originUri)
    val redirectUri = redirectTx.request.clientRequest.getUri
    val redirectRemote = Remote(redirectUri)

    if (redirectRemote == originRemote) {
      sendBufferedRequest(redirectTx.request, redirectRemote)
    } else {
      releaseTokenAndContinue(originRemote, httpCaches.isHttp2PriorKnowledge(session, originRemote).contains(true))
      availableTokensByHost += redirectRemote -> (availableTokensByHost(redirectRemote) - 1)
      if (availableTokensByHost(redirectRemote) > 0) {
        sendBufferedRequest(redirectTx.request, redirectRemote)
      } else {
        bufferedResourcesByHost += redirectRemote -> (redirectTx.request :: bufferedResourcesByHost(redirectRemote))
      }
    }
  }

  override def onCachedResource(uri: Uri, tx: HttpTx): Unit = {
    val remote = Remote(uri)
    resourceFetched(remote, OK, tx.silent, httpCaches.isHttp2PriorKnowledge(tx.session, remote).contains(true))
  }
}
