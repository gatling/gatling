/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.stats.{ KO, Status }
import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.Filters
import io.gatling.core.session._
import io.gatling.http.cache.HttpCaches
import io.gatling.http.client.Request
import io.gatling.http.client.uri.Uri
import io.gatling.http.engine.tx.{ HttpTx, HttpTxExecutor }
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request._
import io.gatling.http.response._
import io.gatling.http.util.HttpHelper._

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.HttpResponseStatus

private[http] object ResourceFetcher extends StrictLogging {
  def applyResourceFilters(resources: List[ConcurrentResource], filters: Option[Filters]): List[ConcurrentResource] =
    filters match {
      case Some(f) => f.filter(resources)
      case _       => resources
    }

  def resourcesToRequests(
      resources: List[ConcurrentResource],
      session: Session,
      httpCaches: HttpCaches,
      httpProtocol: HttpProtocol,
      throttled: Boolean,
      configuration: GatlingConfiguration
  ): List[HttpRequest] =
    resources.flatMap {
      _.toRequest(session, httpCaches, httpProtocol, throttled, configuration) match {
        case Success(httpRequest) => httpRequest :: Nil
        case Failure(m)           =>
          // shouldn't happen, only static values
          logger.error("Couldn't build request for embedded resource: " + m)
          Nil
      }
    }
}

private[http] class ResourceFetcher(
    coreComponents: CoreComponents,
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol,
    httpTxExecutor: HttpTxExecutor
) extends StrictLogging {

  import ResourceFetcher._

  private def inferPageResources(request: Request, response: Response, session: Session, throttled: Boolean): List[HttpRequest] = {

    val htmlDocumentUri = request.getUri

    def inferredResourcesRequests(): List[HttpRequest] = {
      val inferred = new HtmlParser().getEmbeddedResources(htmlDocumentUri, response.body.chars)
      val filtered = applyResourceFilters(inferred, httpProtocol.responsePart.htmlResourcesInferringFilters)
      resourcesToRequests(filtered, session, httpCaches, httpProtocol, throttled, coreComponents.configuration)
    }

    response.status match {
      case HttpResponseStatus.OK =>
        response.lastModifiedOrEtag(httpProtocol) match {
          case Some(lastModifiedOrEtag) =>
            httpCaches.computeInferredResourcesIfAbsent(httpProtocol, htmlDocumentUri, lastModifiedOrEtag, () => inferredResourcesRequests())
          case _ =>
            // don't cache
            inferredResourcesRequests()
        }

      case HttpResponseStatus.NOT_MODIFIED =>
        // no content, retrieve from cache if exist
        httpCaches.getCachedInferredResources(httpProtocol, htmlDocumentUri) match {
          case null =>
            logger.warn(s"Got a 304 for $htmlDocumentUri but could find cache entry?!")
            Nil
          case inferredPageResources => inferredPageResources.requests
        }

      case _ => Nil
    }
  }

  def cssFetched(
      uri: Uri,
      responseStatus: HttpResponseStatus,
      maybeLastModifiedOrEtag: Option[String],
      content: String,
      session: Session,
      throttled: Boolean
  ): List[HttpRequest] = {

    def parseCssResources(): List[HttpRequest] = {
      val computer = CssParser.extractResources(_: Uri, content)
      val inferred = httpCaches.computeCssResourcesIfAbsent(uri, computer)
      val filtered = ResourceFetcher.applyResourceFilters(inferred, httpProtocol.responsePart.htmlResourcesInferringFilters)
      ResourceFetcher.resourcesToRequests(filtered, session, httpCaches, httpProtocol, throttled, coreComponents.configuration)
    }

    // this css might contain some resources
    responseStatus match {
      case HttpResponseStatus.OK =>
        maybeLastModifiedOrEtag match {
          case Some(lastModifiedOrEtag) =>
            httpCaches.computeInferredResourcesIfAbsent(
              httpProtocol,
              uri,
              lastModifiedOrEtag,
              () => {
                httpCaches.removeCssResources(uri)
                parseCssResources()
              }
            )

          case _ =>
            // don't cache
            parseCssResources()
        }

      case HttpResponseStatus.NOT_MODIFIED =>
        // resource was already cached
        httpCaches.getCachedInferredResources(httpProtocol, uri) match {
          case null =>
            logger.warn(s"Got a 304 for $uri but could find cache entry?!")
            Nil
          case inferredPageResources => inferredPageResources.requests
        }
      case _ => Nil
    }
  }

  private def buildExplicitResources(resources: List[HttpRequestDef], session: Session): List[HttpRequest] = resources.flatMap { resource =>
    resource.requestName(session) match {
      case Success(requestName) =>
        resource.build(requestName, session) match {
          case Success(httpRequest) =>
            httpRequest :: Nil

          case Failure(m) =>
            coreComponents.statsEngine.reportUnbuildableRequest(session.scenario, session.groups, requestName, m)
            Nil
        }

      case Failure(m) =>
        logger.error("Could build request name for explicitResource: " + m)
        Nil
    }
  }

  private def resourceAggregator(tx: HttpTx, inferredResources: List[HttpRequest]): Option[ResourceAggregator] = {

    val explicitResources =
      if (tx.request.requestConfig.explicitResources.nonEmpty) {
        buildExplicitResources(tx.request.requestConfig.explicitResources, tx.session)
      } else {
        Nil
      }

    inferredResources ::: explicitResources match {
      case Nil => None
      case resources =>
        Some(new DefaultResourceAggregator(tx, resources, httpCaches, this, httpTxExecutor, coreComponents.clock))
    }
  }

  def newResourceAggregatorForCachedPage(tx: HttpTx): Option[ResourceAggregator] = {
    val inferredResources =
      httpCaches.getCachedInferredResources(tx.request.requestConfig.httpProtocol, tx.request.clientRequest.getUri) match {
        case null      => Nil
        case resources => resources.requests
      }
    resourceAggregator(tx, inferredResources)
  }

  def newResourceAggregatorForFetchedPage(response: Response, tx: HttpTx, status: Status): Option[ResourceAggregator] =
    if (status == KO) {
      None
    } else {
      val inferredResources =
        if (httpProtocol.responsePart.inferHtmlResources && isHtml(response.headers)) {
          inferPageResources(tx.request.clientRequest, response, tx.session, tx.request.requestConfig.throttled)
        } else {
          Nil
        }

      resourceAggregator(tx, inferredResources)
    }
}
