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
 * @param statusCode
 *   the status code of the stored response
 */
private[http] final case class ContentCacheEntry(freshUntil: Option[Long], headers: CacheHeaders, statusCode: Int) {
  def isFresh(nowMillis: Long): Boolean = freshUntil.exists(nowMillis < _)

  // like Chrome, only send validators for stored 200 and 206 responses
  def etag: Option[String] = if (ExpiresSupport.isConditionalizable(statusCode)) headers.etag else None
  def lastModified: Option[String] = if (ExpiresSupport.isConditionalizable(statusCode)) headers.lastModified else None
}

private[cache] object HttpContentCacheSupport {
  val HttpContentCacheAttributeName: String = SessionPrivateAttributes.generatePrivateAttribute("http.cache.contentCache")

  // RFC 9110 §9.2.1
  private val SafeMethods = Set(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE)
  private val GetMethodName = HttpMethod.GET.name
  private val HeadMethodName = HttpMethod.HEAD.name
}

private[cache] trait HttpContentCacheSupport extends ExpiresSupport {
  import HttpContentCacheSupport._

  def configuration: GatlingConfiguration

  private[this] val httpContentCacheHandler =
    new SessionCacheHandler[ContentCacheKey, ContentCacheEntry](HttpContentCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def cacheContent(session: Session, httpProtocol: HttpProtocol, request: Request, status: HttpResponseStatus, responseHeaders: HttpHeaders): Session =
    if (httpProtocol.requestPart.cache && httpContentCacheHandler.enabled) {
      val method = request.getMethod
      if (method == HttpMethod.GET || method == HttpMethod.HEAD) {
        storeResponse(session, ContentCacheKey(request), status, responseHeaders)
      } else if (!SafeMethods.contains(method) && status.code < 400) {
        // RFC 9111 §4.4: a non-error response to an unsafe request invalidates the target URI, whatever the cookies of the stored requests
        val uri = request.getUri
        httpContentCacheHandler.removeEntries(session, key => key.uri == uri && (key.method == GetMethodName || key.method == HeadMethodName))
      } else {
        session
      }
    } else {
      session
    }

  private def storeResponse(session: Session, key: ContentCacheKey, status: HttpResponseStatus, responseHeaders: HttpHeaders): Session = {
    val responseCacheHeaders = CacheHeaders(responseHeaders)

    if (status == HttpResponseStatus.NOT_MODIFIED) {
      // a 304 freshens the stored response it selects, RFC 9111 §4.3.4
      httpContentCacheHandler.getEntry(session, key) match {
        case Some(storedEntry) if storedEntry.headers.isSelectedBy(responseCacheHeaders) =>
          store(session, key, storedEntry.headers.updatedWith(responseCacheHeaders), storedEntry.statusCode, responseHeaders)
        case _ =>
          // no stored response is selected, the 304 must not be used to update anything
          session
      }
    } else {
      store(session, key, responseCacheHeaders, status.code, responseHeaders)
    }
  }

  private def store(
      session: Session,
      key: ContentCacheKey,
      cacheHeaders: CacheHeaders,
      statusCode: Int,
      responseHeaders: HttpHeaders
  ): Session = {
    // a response with Vary: * can never be reused, RFC 9111 §4.1
    // like Chrome, any status code can be stored, what matters is that the response is fresh or can be revalidated
    val storable = !cacheHeaders.noStore && !cacheHeaders.varyAll

    val freshUntilValue = if (storable) freshUntil(cacheHeaders, statusCode, responseHeaders) else None
    val revalidable = ExpiresSupport.isConditionalizable(statusCode) && (cacheHeaders.etag.isDefined || cacheHeaders.lastModified.isDefined)

    if (storable && (freshUntilValue.isDefined || revalidable)) {
      httpContentCacheHandler.addEntry(session, key, ContentCacheEntry(freshUntilValue, cacheHeaders, statusCode))
    } else {
      // the new response replaces the stored one, but can't be reused
      httpContentCacheHandler.removeEntry(session, key)
    }
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
