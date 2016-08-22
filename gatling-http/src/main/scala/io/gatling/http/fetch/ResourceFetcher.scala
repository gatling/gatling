/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import scala.compat.java8.FunctionConverters._

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.TimeHelper._
import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.akka.BaseActor
import io.gatling.core.filter.Filters
import io.gatling.core.session._
import io.gatling.core.util.cache._
import io.gatling.http.action.sync.HttpTx
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.cache.ContentCacheEntry
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol }
import io.gatling.http.request._
import io.gatling.http.response._
import io.gatling.http.util.HttpHelper._

import org.asynchttpclient.Request
import org.asynchttpclient.uri.Uri

sealed trait ResourceFetched {
  def uri: Uri
  def status: Status
  def sessionUpdates: Session => Session
}
case class RegularResourceFetched(uri: Uri, status: Status, sessionUpdates: Session => Session, silent: Boolean) extends ResourceFetched
case class CssResourceFetched(uri: Uri, status: Status, sessionUpdates: Session => Session, silent: Boolean, statusCode: Option[Int], lastModifiedOrEtag: Option[String], content: String) extends ResourceFetched

case class InferredPageResources(expire: String, requests: List[HttpRequest])
case class InferredResourcesCacheKey(protocol: HttpProtocol, uri: Uri)

trait ResourceFetcher {
  self: HttpEngine =>

  // FIXME should CssContentCache use the same key?
  val CssContentCache = Cache.newConcurrentCache[Uri, List[EmbeddedResource]](coreComponents.configuration.http.fetchedCssCacheMaxCapacity)
  val InferredResourcesCache = Cache.newConcurrentCache[InferredResourcesCacheKey, InferredPageResources](coreComponents.configuration.http.fetchedHtmlCacheMaxCapacity)

  def applyResourceFilters(resources: List[EmbeddedResource], filters: Option[Filters]): List[EmbeddedResource] =
    filters match {
      case Some(f) => f.filter(resources)
      case none    => resources
    }

  def resourcesToRequests(resources: List[EmbeddedResource], session: Session, coreComponents: CoreComponents, httpComponents: HttpComponents, throttled: Boolean): List[HttpRequest] =
    resources.flatMap {
      _.toRequest(session, coreComponents, httpComponents, throttled) match {
        case Success(httpRequest) => Some(httpRequest)
        case Failure(m) =>
          // shouldn't happen, only static values
          logger.error("Could build request for embedded resource: " + m)
          None
      }
    }

  private def inferPageResources(request: Request, response: Response, session: Session, config: HttpRequestConfig): List[HttpRequest] = {

    val htmlDocumentUri = response.request.getUri
    val coreComponents = config.coreComponents
    val httpComponents = config.httpComponents
    val httpProtocol = httpComponents.httpProtocol

      def inferredResourcesRequests(): List[HttpRequest] = {
        val inferred = new HtmlParser().getEmbeddedResources(htmlDocumentUri, response.body.string, UserAgent.getAgent(request))
        val filtered = applyResourceFilters(inferred, httpProtocol.responsePart.htmlResourcesInferringFilters)
        resourcesToRequests(filtered, session, coreComponents, httpComponents, config.throttled)
      }

    response.statusCode match {
      case Some(200) =>
        response.lastModifiedOrEtag(httpProtocol) match {
          case Some(newLastModifiedOrEtag) =>
            val cacheKey = InferredResourcesCacheKey(httpProtocol, htmlDocumentUri)
            Option(InferredResourcesCache.get(cacheKey)) match {
              case Some(InferredPageResources(`newLastModifiedOrEtag`, res)) =>
                //cache entry didn't expire, use it
                res
              case _ =>
                // cache entry missing or expired, update it
                val inferredResources = inferredResourcesRequests()
                // FIXME add throttle to cache key?
                InferredResourcesCache.put(cacheKey, InferredPageResources(newLastModifiedOrEtag, inferredResources))
                inferredResources
            }

          case None =>
            // don't cache
            inferredResourcesRequests()
        }

      case Some(304) =>
        // no content, retrieve from cache if exist
        InferredResourcesCache.get(InferredResourcesCacheKey(httpProtocol, htmlDocumentUri)) match {
          case null =>
            logger.warn(s"Got a 304 for $htmlDocumentUri but could find cache entry?!")
            Nil
          case inferredPageResources => inferredPageResources.requests
        }

      case _ => Nil
    }
  }

  private def buildExplicitResources(resources: List[HttpRequestDef], session: Session): List[HttpRequest] = resources.flatMap { resource =>

    resource.requestName(session) match {
      case Success(requestName) => resource.build(requestName, session) match {
        case Success(httpRequest) =>
          Some(httpRequest)

        case Failure(m) =>
          coreComponents.statsEngine.reportUnbuildableRequest(session, requestName, m)
          None
      }

      case Failure(m) =>
        logger.error("Could build request name for explicitResource: " + m)
        None
    }
  }

  private def resourceFetcherActor(tx: HttpTx, inferredResources: List[HttpRequest], explicitResources: List[HttpRequest]) =
    inferredResources ::: explicitResources match {
      case Nil => None
      case resources =>
        implicit val resourceFetcher = this
        Some(() => new ResourceFetcherActor(tx, resources))
    }

  def resourceFetcherActorForCachedPage(htmlDocumentURI: Uri, tx: HttpTx): Option[() => ResourceFetcherActor] = {

    val inferredResources =
      InferredResourcesCache.get(InferredResourcesCacheKey(tx.request.config.httpComponents.httpProtocol, htmlDocumentURI)) match {
        case null      => Nil
        case resources => resources.requests
      }

    val explicitResources = buildExplicitResources(tx.request.config.explicitResources, tx.session)

    resourceFetcherActor(tx, inferredResources, explicitResources)
  }

  def resourceFetcherActorForFetchedPage(request: Request, response: Response, tx: HttpTx): Option[() => ResourceFetcherActor] = {

    val httpProtocol = tx.request.config.httpComponents.httpProtocol

    val explicitResources =
      if (tx.request.config.explicitResources.nonEmpty)
        buildExplicitResources(tx.request.config.explicitResources, tx.session)
      else
        Nil

    val inferredResources =
      if (httpProtocol.responsePart.inferHtmlResources && response.isReceived && isHtml(response.headers))
        inferPageResources(request, response, tx.session, tx.request.config)
      else
        Nil

    resourceFetcherActor(tx, inferredResources, explicitResources)
  }
}

// FIXME handle crash
class ResourceFetcherActor(rootTx: HttpTx, initialResources: Seq[HttpRequest]) extends BaseActor {

  // immutable state
  val coreComponents = rootTx.request.config.coreComponents
  val httpComponents = rootTx.request.config.httpComponents
  import httpComponents._
  val throttled = rootTx.request.config.throttled
  val filters = httpProtocol.responsePart.htmlResourcesInferringFilters

  // mutable state
  var session = rootTx.session
  val alreadySeen = mutable.Set.empty[Uri]
  val bufferedResourcesByHost = mutable.HashMap.empty[String, List[HttpRequest]].withDefaultValue(Nil)
  val availableTokensByHost = mutable.HashMap.empty[String, Int].withDefaultValue(httpProtocol.enginePart.maxConnectionsPerHost)
  var pendingResourcesCount = 0
  var globalStatus: Status = OK
  val start = nowMillis

  // start fetching
  fetchOrBufferResources(initialResources)

  private def fetchResource(resource: HttpRequest): Unit = {
    logger.debug(s"Fetching resource ${resource.ahcRequest.getUri}")

    val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
      resource.config.checks,
      resource.config.responseTransformer,
      httpProtocol.responsePart.discardResponseChunks,
      httpProtocol.responsePart.inferHtmlResources,
      coreComponents.configuration
    )

    val resourceTx = rootTx.copy(
      session = this.session,
      request = resource,
      responseBuilderFactory = responseBuilderFactory,
      resourceFetcher = Some(self)
    )

    HttpTx.start(resourceTx)
  }

  private def handleCachedResource(resource: HttpRequest): Unit = {

    val uri = resource.ahcRequest.getUri

    logger.info(s"Fetching resource $uri from cache")
    // FIXME check if it's a css this way or use the Content-Type?

    val silent = HttpTx.silent(resource, root = false)

    val resourceFetched =
      if (httpEngine.CssContentCache.get(uri) != null)
        CssResourceFetched(uri, OK, Session.Identity, silent, None, None, "")
      else
        RegularResourceFetched(uri, OK, Session.Identity, silent)

    // mock like we've received the resource
    receive(resourceFetched)
  }

  private def fetchOrBufferResources(resources: Iterable[HttpRequest]): Unit = {

      def fetchResources(host: String, resources: Iterable[HttpRequest]): Unit = {
        resources.foreach(fetchResource)
        availableTokensByHost += host -> (availableTokensByHost(host) - resources.size)
      }

      def bufferResources(host: String, resources: Iterable[HttpRequest]): Unit =
        bufferedResourcesByHost += host -> (bufferedResourcesByHost(host) ::: resources.toList)

    alreadySeen ++= resources.map(_.ahcRequest.getUri)
    pendingResourcesCount += resources.size

    val (cached, nonCached) = resources.partition { resource =>
      val ahcRequest = resource.ahcRequest
      httpCaches.contentCacheEntry(session, ahcRequest) match {
        case None => false
        case Some(ContentCacheEntry(Some(expire), _, _)) if unpreciseNowMillis > expire =>
          // beware, side effecting
          session = httpCaches.clearContentCache(session, ahcRequest)
          false
        case _ => true
      }
    }

    cached.foreach(handleCachedResource)

    nonCached
      .groupBy(_.ahcRequest.getUri.getHost)
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
    // FIXME only do so if not silent
    rootTx.next ! session.logGroupRequest((nowMillis - start).toInt, globalStatus)
    context.stop(self)
  }

  private def resourceFetched(uri: Uri, status: Status, silent: Boolean): Unit = {

      def releaseToken(host: String): Unit =
        bufferedResourcesByHost.get(uri.getHost) match {
          case Some(Nil) | None =>
            // nothing to send for this host for now
            availableTokensByHost += host -> (availableTokensByHost(host) + 1)

          case Some(request :: tail) =>
            bufferedResourcesByHost += host -> tail
            val ahcRequest = request.ahcRequest
            httpCaches.contentCacheEntry(session, ahcRequest) match {
              case None =>
                // recycle token, fetch a buffered resource
                fetchResource(request)

              case Some(ContentCacheEntry(Some(expire), _, _)) if unpreciseNowMillis > expire =>
                // expire reached
                session = httpCaches.clearContentCache(session, ahcRequest)
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

  private def cssFetched(uri: Uri, status: Status, statusCode: Option[Int], lastModifiedOrEtag: Option[String], content: String): Unit = {

      def parseCssResources(): List[HttpRequest] = {
        val computer = CssParser.extractResources(_: Uri, content)
        val inferred = httpEngine.CssContentCache.computeIfAbsent(uri, computer.asJava)
        val filtered = httpEngine.applyResourceFilters(inferred, filters)
        httpEngine.resourcesToRequests(filtered, session, coreComponents, httpComponents, throttled)
      }

    if (status == OK) {
      // this css might contain some resources

      val cssResources: List[HttpRequest] =
        statusCode match {
          case Some(200) =>
            lastModifiedOrEtag match {
              case Some(newLastModifiedOrEtag) =>
                // resource can be cached, try to get from cache instead of parsing again

                val cacheKey = InferredResourcesCacheKey(httpProtocol, uri)

                Option(httpEngine.InferredResourcesCache.get(cacheKey)) match {
                  case Some(InferredPageResources(`newLastModifiedOrEtag`, inferredResources)) =>
                    //cache entry didn't expire, use it
                    inferredResources

                  case _ =>
                    // cache entry missing or expired, set/update it
                    httpEngine.CssContentCache.remove(uri)
                    val inferredResources = parseCssResources()
                    httpEngine.InferredResourcesCache.put(InferredResourcesCacheKey(httpProtocol, uri), InferredPageResources(newLastModifiedOrEtag, inferredResources))
                    inferredResources
                }

              case None =>
                // don't cache
                parseCssResources()
            }

          case Some(304) =>
            // resource was already cached
            httpEngine.InferredResourcesCache.get(InferredResourcesCacheKey(httpProtocol, uri)) match {
              case null =>
                logger.warn(s"Got a 304 for $uri but could find cache entry?!")
                Nil
              case inferredPageResources => inferredPageResources.requests
            }
          case _ => Nil
        }

      val filtered = cssResources.filterNot(resource => alreadySeen.contains(resource.ahcRequest.getUri))
      fetchOrBufferResources(filtered)
    }
  }

  def receive: Receive = {
    case RegularResourceFetched(uri, status, sessionUpdates, silent) =>
      session = sessionUpdates(session)
      resourceFetched(uri, status, silent)

    case CssResourceFetched(uri, status, sessionUpdates, silent, statusCode, lastModifiedOrEtag, content) =>
      session = sessionUpdates(session)
      cssFetched(uri, status, statusCode, lastModifiedOrEtag, content)
      resourceFetched(uri, status, silent)
  }
}
