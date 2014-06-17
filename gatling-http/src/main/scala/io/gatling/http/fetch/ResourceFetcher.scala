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
package io.gatling.http.fetch

import java.net.URI

import io.gatling.http.request._

import scala.collection.JavaConversions._
import scala.collection.breakOut
import scala.collection.concurrent
import scala.collection.mutable

import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.core.akka.BaseActor
import io.gatling.core.filter.Filters
import io.gatling.core.result.message.{ OK, Status }
import io.gatling.core.session._
import io.gatling.core.util.StringHelper._
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation._
import io.gatling.http.HeaderNames
import io.gatling.http.action.{ RequestAction, HttpRequestAction }
import io.gatling.http.ahc.HttpTx
import io.gatling.http.cache.CacheHandling
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response._
import jsr166e.ConcurrentHashMapV8

sealed trait ResourceFetched {
  def uri: URI
  def status: Status
  def sessionUpdates: Session => Session
}
case class RegularResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session) extends ResourceFetched
case class CssResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session, statusCode: Option[Int], lastModifiedOrEtag: Option[String], content: String) extends ResourceFetched

case class InferredPageResources(expire: String, requests: List[HttpRequest])

object ResourceFetcher extends StrictLogging {

  val CssContentCache: concurrent.Map[URI, List[EmbeddedResource]] = new ConcurrentHashMapV8[URI, List[EmbeddedResource]]
  val InferredResourcesCache: concurrent.Map[(HttpProtocol, URI), InferredPageResources] = new ConcurrentHashMapV8[(HttpProtocol, URI), InferredPageResources]

  def pageResources(htmlDocumentURI: URI, filters: Option[Filters], responseChars: Array[Char]): List[EmbeddedResource] = {
    val htmlInferredResources = new HtmlParser().getEmbeddedResources(htmlDocumentURI, responseChars)
    filters match {
      case Some(f) => f.filter(htmlInferredResources)
      case none    => htmlInferredResources
    }
  }

  def cssResources(cssURI: URI, filters: Option[Filters], content: String): List[EmbeddedResource] = {
    val cssInferredResources = CssContentCache.getOrElseUpdate(cssURI, CssParser.extractResources(cssURI, content))
    filters match {
      case Some(f) => f.filter(cssInferredResources)
      case none    => cssInferredResources
    }
  }

  def lastModifiedOrEtag(response: Response, protocol: HttpProtocol): Option[String] =
    if (protocol.requestPart.cache)
      response.header(HeaderNames.LastModified).orElse(response.header(HeaderNames.ETag))
    else
      None

  def resourcesToRequests(resources: List[EmbeddedResource], protocol: HttpProtocol, throttled: Boolean): List[HttpRequest] =
    resources.flatMap {
      _.toRequest(protocol, throttled) match {
        case Success(httpRequest) => Some(httpRequest)
        case Failure(m) =>
          // shouldn't happen
          logger.error("Could build request for embedded resource: " + m)
          None
      }
    }

  def resourcesFromPage(response: Response, tx: HttpTx): List[HttpRequest] = {

    val htmlDocumentURI = response.request.getURI
    val protocol = tx.request.config.protocol

      def inferredResourcesRequests(): List[HttpRequest] = {
        val res = pageResources(htmlDocumentURI, protocol.responsePart.htmlResourcesInferringFilters, response.body.string.unsafeChars)
        resourcesToRequests(res, protocol, tx.request.config.throttled)
      }

    val inferredResources: List[HttpRequest] = response.statusCode match {
      case Some(200) =>
        lastModifiedOrEtag(response, protocol) match {
          case Some(newLastModifiedOrEtag) =>
            val cacheKey = (protocol, htmlDocumentURI)
            InferredResourcesCache.get(cacheKey) match {
              case Some(InferredPageResources(`newLastModifiedOrEtag`, res)) =>
                //cache entry didn't expire, use it
                res
              case _ =>
                // cache entry missing or expired, update it
                val inferredResources = inferredResourcesRequests()
                // FIXME add throttle to cache key?
                InferredResourcesCache.put((protocol, htmlDocumentURI), InferredPageResources(newLastModifiedOrEtag, inferredResources))
                inferredResources
            }

          case None =>
            // don't cache
            inferredResourcesRequests()
        }

      case Some(304) =>
        // no content, retrieve from cache if exist
        val cacheKey = (protocol, htmlDocumentURI)
        InferredResourcesCache.get(cacheKey) match {
          case Some(inferredPageResources) => inferredPageResources.requests
          case _ =>
            logger.warn(s"Got a 304 for $htmlDocumentURI but could find cache entry?!")
            Nil
        }

      case _ => Nil
    }

    inferredResources
  }

  def fetchResources(tx: HttpTx, explicitResources: List[HttpRequest]): Option[() => ResourceFetcher] =
    resourceFetcher(tx, Nil, explicitResources)

  def buildExplicitResources(resources: List[HttpRequestDef], session: Session): List[HttpRequest] = resources.flatMap { resource =>

    resource.requestName(session) match {
      case Success(requestName) => resource.build(requestName, session) match {
        case Success(httpRequest) =>
          Some(httpRequest)

        case Failure(m) =>
          RequestAction.reportUnbuildableRequest(requestName, session, m)
          None
      }

      case Failure(m) =>
        logger.error("Could build request name for explicitResource: " + m)
        None
    }
  }

  def fromCache(htmlDocumentURI: URI, tx: HttpTx): Option[() => ResourceFetcher] = {

    val inferredResources = {
      val cacheKey = (tx.request.config.protocol, htmlDocumentURI)
      InferredResourcesCache.get(cacheKey).map(_.requests).getOrElse(Nil)
    }

    val explicitResources = buildExplicitResources(tx.request.config.explicitResources, tx.session)

    resourceFetcher(tx, inferredResources, explicitResources)
  }

  def resourceFetcher(tx: HttpTx, inferredResources: List[HttpRequest], explicitResources: List[HttpRequest]) = {

    val uniqueResources: Map[URI, HttpRequest] = {
      val inf: Map[URI, HttpRequest] = inferredResources.map(res => res.ahcRequest.getURI -> res)(breakOut)
      val exp: Map[URI, HttpRequest] = explicitResources.map(res => res.ahcRequest.getURI -> res)(breakOut)
      inf ++ exp
    }

    if (uniqueResources.isEmpty)
      None
    else {
      Some(() => new ResourceFetcher(tx, uniqueResources.values))
    }
  }
}

// FIXME handle crash
class ResourceFetcher(tx: HttpTx, initialResources: Iterable[HttpRequest]) extends BaseActor {

  import ResourceFetcher._

  var session = tx.session
  val alreadySeen: Set[URI] = initialResources.map(_.ahcRequest.getURI).toSet
  val bufferedRequestsByHost = mutable.HashMap.empty[String, List[HttpRequest]].withDefaultValue(Nil)
  val availableTokensByHost = mutable.HashMap.empty[String, Int].withDefaultValue(tx.request.config.protocol.enginePart.maxConnectionsPerHost)
  var pendingRequestsCount = initialResources.size
  var okCount = 0
  var koCount = 0
  val start = nowMillis

  // start fetching
  fetchOrBufferResources(initialResources)

  def fetchResource(request: HttpRequest): Unit = {
    logger.debug(s"Fetching resource ${request.ahcRequest.getUrl}")

    val resourceTx = tx.copy(
      session = this.session,
      request = request,
      responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(request.config.checks, None, request.config.protocol),
      next = self,
      secondary = true)

    HttpRequestAction.startHttpTransaction(resourceTx)
  }

  def handleCachedRequest(request: HttpRequest): Unit = {
    logger.info(s"Fetching resource ${request.ahcRequest.getURI} from cache")
    // FIXME check if it's a css this way or use the Content-Type?
    val resourceFetched = if (CssContentCache.contains(request.ahcRequest.getURI))
      CssResourceFetched(request.ahcRequest.getURI, OK, identity, None, None, "")
    else
      RegularResourceFetched(request.ahcRequest.getURI, OK, identity)

    receive(resourceFetched)
  }

  def fetchOrBufferResources(requests: Iterable[HttpRequest]): Unit = {

      def sendRequests(host: String, requests: Iterable[HttpRequest]): Unit = {
        requests.foreach(fetchResource)
        availableTokensByHost += host -> (availableTokensByHost(host) - requests.size)
      }

      def bufferRequests(host: String, requests: Iterable[HttpRequest]): Unit =
        bufferedRequestsByHost += host -> (bufferedRequestsByHost(host) ::: requests.toList)

    val (nonCachedRequests, cachedRequests) = requests.partition { request =>
      val uri = request.ahcRequest.getURI
      CacheHandling.getExpire(tx.request.config.protocol, session, uri) match {
        case None => true
        case Some(expire) if nowMillis > expire =>
          // ugly, side effecting
          session = CacheHandling.clearExpire(session, uri)
          true
        case _ => false
      }
    }

    cachedRequests.foreach(handleCachedRequest)

    nonCachedRequests
      .groupBy(_.ahcRequest.getURI.getHost)
      .foreach {
        case (host, reqs) =>
          val availableTokens = availableTokensByHost(host)
          val (immediateRequests, bufferedRequests) = reqs.splitAt(availableTokens)
          sendRequests(host, immediateRequests)
          bufferRequests(host, bufferedRequests)
      }
  }

  private def done(): Unit = {
    logger.debug("All resources were fetched")
    tx.next ! session.logGroupAsyncRequests(nowMillis - start, okCount, koCount)
    context.stop(self)
  }

  def resourceFetched(uri: URI, status: Status): Unit = {

      def releaseToken(host: String, bufferedRequests: List[HttpRequest]): Unit =
        bufferedRequests match {
          case Nil =>
            // nothing to send for this host
            availableTokensByHost += host -> (availableTokensByHost(host) + 1)

          case request :: tail =>
            bufferedRequestsByHost += host -> tail
            val uri = request.ahcRequest.getURI
            CacheHandling.getExpire(tx.request.config.protocol, session, uri) match {
              case None =>
                // recycle token, fetch a buffered resource
                fetchResource(request)

              case Some(expire) if nowMillis > expire =>
                // expire reached
                session = CacheHandling.clearExpire(session, uri)
                fetchResource(request)

              case _ =>
                handleCachedRequest(request)
                releaseToken(host, tail)
            }
        }

    logger.debug(s"Resource $uri was fetched")
    pendingRequestsCount -= 1

    if (status == OK)
      okCount = okCount + 1
    else
      koCount = koCount + 1

    if (pendingRequestsCount == 0)
      done()
    else {
      val requests = bufferedRequestsByHost.get(uri.getHost) match {
        case Some(reqs) => reqs
        case _          => Nil
      }
      releaseToken(uri.getHost, requests)
    }
  }

  def cssFetched(uri: URI, status: Status, statusCode: Option[Int], lastModifiedOrEtag: Option[String], content: String): Unit = {

    val protocol = tx.request.config.protocol

    if (status == OK) {
      // this css might contain some resources

      val rawCssResources: List[HttpRequest] = statusCode match {
        case Some(200) =>
          // try to get from cache
          lastModifiedOrEtag match {
            case Some(newLastModifiedOrEtag) =>
              val cacheKey = (protocol, uri)
              InferredResourcesCache.get(cacheKey) match {
                case Some(InferredPageResources(`newLastModifiedOrEtag`, inferredResources)) =>
                  //cache entry didn't expire, use it
                  inferredResources
                case _ =>
                  // cache entry missing or expired, update it
                  CssContentCache.remove(protocol -> uri)
                  val inferredResources = {
                    val res = cssResources(uri, protocol.responsePart.htmlResourcesInferringFilters, content)
                    resourcesToRequests(res, protocol, tx.request.config.throttled)
                  }
                  InferredResourcesCache.put((protocol, uri), InferredPageResources(newLastModifiedOrEtag, inferredResources))
                  inferredResources
              }

            case None =>
              // don't cache
              val res = cssResources(uri, protocol.responsePart.htmlResourcesInferringFilters, content)
              resourcesToRequests(res, protocol, tx.request.config.throttled)
          }

        case Some(304) =>
          // no content, retrieve from cache if exist
          val cacheKey = (protocol, uri)
          InferredResourcesCache.get(cacheKey) match {
            case Some(inferredPageResources) => inferredPageResources.requests
            case _ =>
              logger.warn(s"Got a 304 for $uri but could find cache entry?!")
              Nil
          }
        case _ => Nil
      }

      val filtered = rawCssResources.filterNot(res => alreadySeen.contains(res.ahcRequest.getURI))

      pendingRequestsCount += filtered.size
      fetchOrBufferResources(filtered)
    }
  }

  def receive: Receive = {
    case RegularResourceFetched(uri, status, sessionUpdates) =>
      session = sessionUpdates(session)
      resourceFetched(uri, status)

    case CssResourceFetched(uri, status, sessionUpdates, statusCode, lastModifiedOrEtag, content) =>
      session = sessionUpdates(session)
      cssFetched(uri, status, statusCode, lastModifiedOrEtag, content)
      resourceFetched(uri, status)
  }
}
