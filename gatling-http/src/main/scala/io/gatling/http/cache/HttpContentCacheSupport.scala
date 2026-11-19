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

import scala.jdk.CollectionConverters._

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.cache.SessionCacheHandler
import io.gatling.http.client.Request
import io.gatling.http.client.uri.Uri
import io.gatling.http.protocol.HttpProtocol

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaderValues, HttpHeaders, HttpMethod, HttpResponseStatus }

object ContentCacheKey {
  private val GetMethodName = HttpMethod.GET.name

  // like Chrome, HEAD requests use the stored GET response
  def apply(request: Request): ContentCacheKey = {
    val method = request.getMethod
    new ContentCacheKey(request.getUri, if (method == HttpMethod.HEAD) GetMethodName else method.name, Cookies(request.getCookies))
  }
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

/**
 * How the cache directives and conditionals set by the user on a request affect the cache, like Chrome does (see `GetLoadFlagsForExtraHeaders` in Chromium's
 * `net/http/http_cache_util.cc`, and Chrome's behavior for user-supplied validators).
 */
private[cache] object RequestCacheHeaders {
  private val NoCache = HttpHeaderValues.NO_CACHE.toString
  private val MaxAgeZero = "max-age=0"

  // the cache must neither be read nor written, as reusing a stored response would cause unexpected 412s
  private val PassThroughHeaderNames = List(HttpHeaderNames.IF_UNMODIFIED_SINCE, HttpHeaderNames.IF_MATCH, HttpHeaderNames.IF_RANGE)

  private def hasToken(headers: HttpHeaders, name: CharSequence, token: String): Boolean =
    headers.contains(name) && headers.getAll(name).asScala.exists(HeaderTokens.hasToken(_, token))

  def passThrough(headers: HttpHeaders): Boolean = PassThroughHeaderNames.exists(headers.contains)

  // the stored response must not be reused, nor revalidated, but the new response can be stored
  def forceFetch(headers: HttpHeaders): Boolean =
    hasToken(headers, HttpHeaderNames.CACHE_CONTROL, NoCache) || hasToken(headers, HttpHeaderNames.PRAGMA, NoCache)

  // the stored response must be revalidated before being reused
  def forceValidate(headers: HttpHeaders): Boolean = hasToken(headers, HttpHeaderNames.CACHE_CONTROL, MaxAgeZero)

  def hasValidators(headers: HttpHeaders): Boolean =
    headers.contains(HttpHeaderNames.IF_NONE_MATCH) || headers.contains(HttpHeaderNames.IF_MODIFIED_SINCE)

  // the response to a request with validators can only update the stored response they identify
  def validatorsMatch(headers: HttpHeaders, storedEntry: Option[ContentCacheEntry]): Boolean = {
    def matches(requestHeaderName: CharSequence, storedValue: CacheHeaders => Option[String]): Boolean =
      Option(headers.get(requestHeaderName)).forall(value => storedEntry.exists(entry => storedValue(entry.headers).contains(value)))

    matches(HttpHeaderNames.IF_NONE_MATCH, _.etag) && matches(HttpHeaderNames.IF_MODIFIED_SINCE, _.lastModified)
  }
}

private[http] object HttpContentCacheSupport {

  /**
   * Remove the validators added by the cache from a follow-up request, as they belong to the stored response of the original request. The follow-up request
   * must have its own copy of the headers.
   */
  def removeValidators(request: Request): Request = {
    request.getHeaders.remove(HttpHeaderNames.IF_NONE_MATCH).remove(HttpHeaderNames.IF_MODIFIED_SINCE)
    request
  }

  val HttpContentCacheAttributeName: String = SessionPrivateAttributes.generatePrivateAttribute("http.cache.contentCache")

  // RFC 9110 §9.2.1
  private val SafeMethods = Set(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE)
  private val GetMethodName = HttpMethod.GET.name
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
        val requestHeaders = request.getHeaders
        val cacheExists = httpContentCacheHandler.cacheExists(session)
        lazy val cacheControl = CacheHeaders.cacheControl(responseHeaders)
        val isHead = method == HttpMethod.HEAD
        if (!cacheExists && (isHead || !CacheHeaders.mayBeStored(status, responseHeaders) || cacheControl.exists(_.noStore))) {
          // fast path, typically for API calls: nothing stored to update, nothing to store
          session
        } else if (RequestCacheHeaders.passThrough(requestHeaders)) {
          session
        } else {
          val key = ContentCacheKey(request)
          val hasValidators = RequestCacheHeaders.hasValidators(requestHeaders)
          if (hasValidators && !RequestCacheHeaders.validatorsMatch(requestHeaders, httpContentCacheHandler.getEntry(session, key))) {
            // the user's validators don't identify the stored response
            session
          } else if (isHead) {
            // like Chrome, HEAD responses are never stored, but when revalidating the stored GET response, a 304 freshens it while other responses drop it
            if (!hasValidators) {
              session
            } else if (status == HttpResponseStatus.NOT_MODIFIED) {
              storeResponse(session, key, status, responseHeaders, CacheHeaders(responseHeaders, cacheControl))
            } else {
              httpContentCacheHandler.removeEntry(session, key)
            }
          } else {
            storeResponse(session, key, status, responseHeaders, CacheHeaders(responseHeaders, cacheControl))
          }
        }
      } else if (
        httpContentCacheHandler.cacheExists(session) && status.code < 400 && !SafeMethods.contains(method) &&
        // like Chrome, requests with If-Match, If-Unmodified-Since or If-Range bypass the cache, including invalidation
        !RequestCacheHeaders.passThrough(request.getHeaders)
      ) {
        // RFC 9111 §4.4: a non-error response to an unsafe request invalidates the target URI, HEAD requests using the stored GET response.
        // Stored responses are keyed with the request cookies, and the ones of the unsafe request are the ones a subsequent request would use.
        httpContentCacheHandler.removeEntry(session, ContentCacheKey(request).copy(method = GetMethodName))
      } else {
        session
      }
    } else {
      session
    }

  private def storeResponse(
      session: Session,
      key: ContentCacheKey,
      status: HttpResponseStatus,
      responseHeaders: HttpHeaders,
      responseCacheHeaders: CacheHeaders
  ): Session =

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
   *   true if a fresh response is stored and the request allows reusing it, meaning the request doesn't have to be sent. Stale responses are kept so their
   *   validators can be used for revalidation, RFC 9111 §4.3.
   */
  def isFresh(session: Session, request: Request): Boolean =
    contentCacheEntry(session, request).exists(_.isFresh(clock.nowMillis)) && {
      val headers = request.getHeaders
      // validators can only be the user's here, as the cache only adds its own when the stored response is stale or has to be revalidated
      !RequestCacheHeaders.passThrough(headers) &&
      !RequestCacheHeaders.forceFetch(headers) &&
      !RequestCacheHeaders.forceValidate(headers) &&
      !RequestCacheHeaders.hasValidators(headers)
    }

  /**
   * Add the validators of the stored response to a request that's about to be sent, when it has to be revalidated, unless the user's own request headers forbid
   * it. Like Chrome, this is performed for every hop, so validators added for a request must be removed from its follow-ups, see
   * `HttpContentCacheSupport.removeValidators`.
   *
   * @return
   *   a copy of the request with the validators, as requests, such as the inferred resources ones, can be shared amongst virtual users and must never be
   *   mutated, or None if no validators were added
   */
  def withValidators(session: Session, request: Request): Option[Request] =
    contentCacheEntry(session, request) match {
      case Some(entry) if entry.etag.isDefined || entry.lastModified.isDefined =>
        val headers = request.getHeaders
        if (
          !RequestCacheHeaders.passThrough(headers) && !RequestCacheHeaders.forceFetch(headers) && !RequestCacheHeaders.hasValidators(headers) &&
          // a fresh response will be reused, unless the user forces revalidation
          (!entry.isFresh(clock.nowMillis) || RequestCacheHeaders.forceValidate(headers))
        ) {
          val requestWithValidators = request.copyWithCopiedHeaders()
          entry.etag.foreach(requestWithValidators.getHeaders.set(HttpHeaderNames.IF_NONE_MATCH, _))
          entry.lastModified.foreach(requestWithValidators.getHeaders.set(HttpHeaderNames.IF_MODIFIED_SINCE, _))
          Some(requestWithValidators)
        } else {
          None
        }
      case _ => None
    }
}
