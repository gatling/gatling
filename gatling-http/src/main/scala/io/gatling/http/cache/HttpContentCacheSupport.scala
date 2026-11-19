/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.http.cache

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.cache.SessionCacheHandler
import io.gatling.http.client.Request
import io.gatling.http.client.uri.Uri
import io.gatling.http.protocol.HttpProtocol

import io.netty.handler.codec.http.{ HttpHeaders, HttpMethod, HttpResponseStatus }

object ContentCacheKey {
  def apply(request: Request): ContentCacheKey =
    new ContentCacheKey(request.getUri, request.getMethod.name, Cookies(request.getCookies))
}

private[cache] final case class ContentCacheKey(uri: Uri, method: String, cookies: Map[String, String])

/**
 * @param freshUntil
 *   the timestamp until which the stored response can be reused without contacting the server, None if it has to be revalidated
 * @param headers
 *   the caching header fields of the stored response
 * @param heuristicallyCacheable
 *   if the status code of the stored response allows for heuristic freshness
 */
private[http] final case class ContentCacheEntry(freshUntil: Option[Long], headers: CacheHeaders, heuristicallyCacheable: Boolean) {
  def etag: Option[String] = headers.etag
  def lastModified: Option[String] = headers.lastModified
  def isFresh(nowMillis: Long): Boolean = freshUntil.exists(nowMillis < _)
}

private[cache] object HttpContentCacheSupport {
  val HttpContentCacheAttributeName: String = SessionPrivateAttributes.generatePrivateAttribute("http.cache.contentCache")
}

private[cache] trait HttpContentCacheSupport extends ExpiresSupport {
  import HttpContentCacheSupport._

  def configuration: GatlingConfiguration

  private[this] val httpContentCacheHandler =
    new SessionCacheHandler[ContentCacheKey, ContentCacheEntry](HttpContentCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def cacheContent(session: Session, httpProtocol: HttpProtocol, request: Request, status: HttpResponseStatus, responseHeaders: HttpHeaders): Session =
    if (httpProtocol.requestPart.cache && httpContentCacheHandler.enabled) {
      val key = ContentCacheKey(request)
      val responseCacheHeaders = CacheHeaders(responseHeaders)

      // a 304 freshens the stored response, RFC 9111 §4.3.4
      val storedEntry = if (status == HttpResponseStatus.NOT_MODIFIED) httpContentCacheHandler.getEntry(session, key) else None
      val cacheHeaders = storedEntry.fold(responseCacheHeaders)(_.headers.updatedWith(responseCacheHeaders))
      val heuristicallyCacheable = storedEntry.fold(ExpiresSupport.isHeuristicallyCacheable(status))(_.heuristicallyCacheable)

      // RFC 9111 §3
      val storable = (request.getMethod == HttpMethod.GET || request.getMethod == HttpMethod.HEAD) &&
        !cacheHeaders.noStore &&
        (heuristicallyCacheable || cacheHeaders.allowsStorage)

      val freshUntilValue = if (storable) freshUntil(cacheHeaders, heuristicallyCacheable, responseHeaders) else None

      if (storable && (freshUntilValue.isDefined || cacheHeaders.etag.isDefined || cacheHeaders.lastModified.isDefined)) {
        httpContentCacheHandler.addEntry(session, key, ContentCacheEntry(freshUntilValue, cacheHeaders, heuristicallyCacheable))
      } else {
        // the new response replaces the stored one, but can't be reused
        httpContentCacheHandler.removeEntry(session, key)
      }
    } else {
      session
    }

  def contentCacheEntry(session: Session, request: Request): Option[ContentCacheEntry] =
    if (httpContentCacheHandler.enabled) {
      httpContentCacheHandler.getEntry(session, ContentCacheKey(request))
    } else {
      None
    }

  /**
   * @return
   *   true if a fresh response is stored, meaning the request doesn't have to be sent. Stale responses are kept so their validators can be used for
   *   revalidation, RFC 9111 §4.3.
   */
  def isFresh(session: Session, request: Request): Boolean =
    contentCacheEntry(session, request).exists(_.isFresh(clock.nowMillis))
}
