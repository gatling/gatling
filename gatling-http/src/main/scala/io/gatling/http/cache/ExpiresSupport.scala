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

import io.gatling.commons.util.Clock

import io.netty.handler.codec.DateFormatter
import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaderValues, HttpHeaders, HttpResponseStatus }

/**
 * Splits comma-separated header values, ignoring commas in quoted strings and trimming whitespace, like Chromium's `HttpUtil::ValuesIterator`.
 */
private[cache] object HeaderTokens {
  private def isWhitespace(c: Char): Boolean = c == ' ' || c == '\t'

  /**
   * Iterate over the tokens as [start, end) index ranges, so no substring gets allocated.
   *
   * @return
   *   true as soon as a token satisfies the predicate, which is also how to iterate over all the tokens
   */
  def exists(value: String)(p: (Int, Int) => Boolean): Boolean =
    if (value.indexOf('"') < 0) existsUnquoted(value, p) else existsQuoted(value, p)

  private def trimmedTokenMatches(value: String, start: Int, end: Int, p: (Int, Int) => Boolean): Boolean = {
    var s = start
    var e = end
    while (s < e && isWhitespace(value.charAt(s))) s += 1
    while (e > s && isWhitespace(value.charAt(e - 1))) e -= 1
    e > s && p(s, e)
  }

  // fast path for the common case: no quoted strings, so commas can be looked up with the vectorized indexOf
  private def existsUnquoted(value: String, p: (Int, Int) => Boolean): Boolean = {
    var found = false
    var start = 0
    while (!found && start <= value.length) {
      val comma = value.indexOf(',', start)
      val end = if (comma < 0) value.length else comma
      found = trimmedTokenMatches(value, start, end, p)
      start = end + 1
    }
    found
  }

  private def existsQuoted(value: String, p: (Int, Int) => Boolean): Boolean = {
    var found = false
    var start = 0
    var inQuotes = false
    var i = 0
    while (!found && i <= value.length) {
      val c = if (i < value.length) value.charAt(i) else ','
      if (c == '\\' && inQuotes && i + 1 < value.length) {
        i += 1 // skip the escaped char
      } else if (c == '"') {
        inQuotes = !inQuotes
      } else if (c == ',' && (!inQuotes || i == value.length)) {
        found = trimmedTokenMatches(value, start, i, p)
        start = i + 1
      }
      i += 1
    }
    found
  }

  // case-insensitive, trying the common lower case first as it's way cheaper
  def matches(value: String, start: Int, end: Int, token: String): Boolean =
    end - start == token.length && (value.startsWith(token, start) || value.regionMatches(true, start, token, 0, token.length))

  def hasToken(value: String, token: String): Boolean = exists(value)(matches(value, _, _, token))
}

/**
 * The Cache-Control directives that drive Gatling's caching decisions, parsed like Chrome does, see Chromium's `HttpResponseHeaders::HasCacheRestriction` and
 * `HttpResponseHeaders::ParseCacheControlDirectivesForFreshness`.
 *
 * @param maxAge
 *   the first valid max-age, invalid ones (including quoted values and spaces around the equals sign) are ignored
 */
private[http] final case class CacheControl(noStore: Boolean, noCache: Boolean, mustRevalidate: Boolean, maxAge: Option[Long])

private[http] object CacheControl {
  private val MaxAgePrefix = "max-age="

  def parse(value: String): CacheControl = {
    var noStore = false
    var noCache = false
    var mustRevalidate = false
    var maxAge: Option[Long] = None

    // directives with arguments such as no-cache="Set-Cookie" or no-store="foo" are not exact matches, hence ignored
    HeaderTokens.exists(value) { (start, end) =>
      // dispatch on the first char, so most tokens are only compared once
      value.charAt(start) match {
        case 'n' | 'N' =>
          if (HeaderTokens.matches(value, start, end, "no-store")) {
            noStore = true
          } else if (HeaderTokens.matches(value, start, end, "no-cache")) {
            noCache = true
          }
        case 'm' | 'M' =>
          if (HeaderTokens.matches(value, start, end, "must-revalidate")) {
            mustRevalidate = true
          } else if (
            maxAge.isEmpty && end - start >= MaxAgePrefix.length &&
            (value.startsWith(MaxAgePrefix, start) || value.regionMatches(true, start, MaxAgePrefix, 0, MaxAgePrefix.length))
          ) {
            maxAge = ExpiresSupport.parseDeltaSeconds(value, start + MaxAgePrefix.length, end)
          }
        case _ =>
      }
      false
    }

    CacheControl(noStore, noCache, mustRevalidate, maxAge)
  }
}

/**
 * The header fields of a stored response that drive caching decisions (RFC 9111).
 */
private[http] final case class CacheHeaders(
    cacheControl: Option[CacheControl],
    expires: Option[String],
    pragma: Option[String],
    etag: Option[String],
    lastModified: Option[String],
    vary: Option[String]
) {
  private[cache] def noCache: Boolean = cacheControl.exists(_.noCache)

  private[cache] def noStore: Boolean = cacheControl.exists(_.noStore)

  private[cache] def mustRevalidate: Boolean = cacheControl.exists(_.mustRevalidate)

  private[cache] def maxAge: Option[Long] = cacheControl.flatMap(_.maxAge)

  // RFC 9111 §5.4 deprecates Pragma, but Chrome still honors it on responses, as a case-insensitive token
  private[cache] def pragmaNoCache: Boolean = pragma.exists(HeaderTokens.hasToken(_, HttpHeaderValues.NO_CACHE.toString))

  // RFC 9111 §4.1: a stored response with Vary: * always fails to match
  private[cache] def varyAll: Boolean = vary.exists(HeaderTokens.hasToken(_, "*"))

  /**
   * Check if a 304 response selects this stored response, RFC 9111 §4.3.4.
   */
  def isSelectedBy(notModified: CacheHeaders): Boolean =
    notModified.etag match {
      case Some(newEtag) if newEtag.startsWith(ExpiresSupport.WeakEtagPrefix) =>
        // weak comparison, RFC 9110 §8.8.3.2
        etag.exists(storedEtag => ExpiresSupport.opaqueTag(storedEtag) == ExpiresSupport.opaqueTag(newEtag))
      case Some(newEtag) =>
        // strong comparison, a weak stored ETag can't match as it keeps its W/ prefix
        etag.contains(newEtag)
      case _ =>
        notModified.lastModified match {
          case Some(newLastModified) => lastModified.contains(newLastModified)
          case _                     => true
        }
    }

  /**
   * Update the header fields of a stored response with the ones of a 304 response, RFC 9111 §4.3.4.
   */
  def updatedWith(notModified: CacheHeaders): CacheHeaders =
    CacheHeaders(
      cacheControl = notModified.cacheControl.orElse(cacheControl),
      expires = notModified.expires.orElse(expires),
      pragma = notModified.pragma.orElse(pragma),
      etag = notModified.etag.orElse(etag),
      lastModified = notModified.lastModified.orElse(lastModified),
      vary = notModified.vary.orElse(vary)
    )
}

private[http] object CacheHeaders {
  // multiple field lines are combined into a single comma-separated list, RFC 9110 §5.3
  private def combined(headers: HttpHeaders, name: CharSequence): Option[String] =
    if (!headers.contains(name)) {
      None
    } else {
      val values = headers.getAll(name)
      if (values.size == 1) Some(values.get(0)) else Some(String.join(",", values))
    }

  /**
   * @return
   *   false if the response can't be stored for sure, typically for API calls
   */
  def cacheControl(headers: HttpHeaders): Option[CacheControl] = combined(headers, HttpHeaderNames.CACHE_CONTROL).map(CacheControl.parse)

  def mayBeStored(status: HttpResponseStatus, headers: HttpHeaders): Boolean =
    headers.contains(HttpHeaderNames.CACHE_CONTROL) ||
      headers.contains(HttpHeaderNames.EXPIRES) ||
      headers.contains(HttpHeaderNames.ETAG) ||
      headers.contains(HttpHeaderNames.LAST_MODIFIED) ||
      ExpiresSupport.isImplicitlyFresh(status.code)

  def apply(headers: HttpHeaders): CacheHeaders = apply(headers, cacheControl(headers))

  def apply(headers: HttpHeaders, cacheControl: Option[CacheControl]): CacheHeaders =
    new CacheHeaders(
      cacheControl = cacheControl,
      expires = Option(headers.get(HttpHeaderNames.EXPIRES)),
      pragma = Option(headers.get(HttpHeaderNames.PRAGMA)),
      etag = Option(headers.get(HttpHeaderNames.ETAG)),
      lastModified = Option(headers.get(HttpHeaderNames.LAST_MODIFIED)),
      vary = combined(headers, HttpHeaderNames.VARY)
    )
}

private[cache] object ExpiresSupport {
  // RFC 9111 §1.2.2
  private val MaxDeltaSeconds = 2147483648L

  // Chrome only computes heuristic freshness for those, see HttpResponseHeaders::GetFreshnessLifetimes.
  // This is narrower than RFC 9110 §15.1's list of heuristically cacheable status codes.
  private val HeuristicFreshnessStatusCodes: Set[Int] = Set(
    HttpResponseStatus.OK.code,
    HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION.code,
    HttpResponseStatus.PARTIAL_CONTENT.code
  )

  // Chrome considers those fresh forever when there's no explicit expiration time, see HttpResponseHeaders::GetFreshnessLifetimes
  private val ImplicitlyFreshStatusCodes: Set[Int] = Set(
    HttpResponseStatus.MULTIPLE_CHOICES.code,
    HttpResponseStatus.MOVED_PERMANENTLY.code,
    HttpResponseStatus.PERMANENT_REDIRECT.code,
    HttpResponseStatus.GONE.code
  )

  // Chrome only revalidates stored 200 and 206 responses, see HttpCache::Transaction::IsResponseConditionalizable
  private val ConditionalizableStatusCodes: Set[Int] = Set(HttpResponseStatus.OK.code, HttpResponseStatus.PARTIAL_CONTENT.code)

  val FreshForever: Long = Long.MaxValue

  // RFC 9111 §4.2.2: "if the response has a Last-Modified header field, caches are encouraged to use a heuristic expiration value
  // that is no more than some fraction of the interval since that time. A typical setting of this fraction might be 10%."
  private val HeuristicFraction = 10

  val WeakEtagPrefix = "W/"

  def opaqueTag(etag: String): String = if (etag.startsWith(WeakEtagPrefix)) etag.substring(WeakEtagPrefix.length) else etag

  def isImplicitlyFresh(statusCode: Int): Boolean = ImplicitlyFreshStatusCodes.contains(statusCode)

  def isConditionalizable(statusCode: Int): Boolean = ConditionalizableStatusCodes.contains(statusCode)

  private def unquote(s: String): String =
    if (s.length >= 2 && s.charAt(0) == '"' && s.charAt(s.length - 1) == '"') s.substring(1, s.length - 1) else s

  // RFC 9111 §1.2.2, surrounding spaces and leading zeros are ignored, like Chrome
  def parseDeltaSeconds(s: String): Option[Long] = parseDeltaSeconds(s, 0, s.length)

  def parseDeltaSeconds(value: String, start: Int, end: Int): Option[Long] = {
    var s = start
    var e = end
    while (s < e && value.charAt(s) == ' ') s += 1
    while (e > s && value.charAt(e - 1) == ' ') e -= 1

    if (s == e) {
      None
    } else {
      var allDigits = true
      var seconds = 0L
      var i = s
      while (allDigits && i < e) {
        val c = value.charAt(i)
        if (c >= '0' && c <= '9') {
          // capping on the way prevents overflowing
          seconds = math.min(seconds * 10 + (c - '0'), MaxDeltaSeconds)
        } else {
          allDigits = false
        }
        i += 1
      }
      Option.when(allDigits)(seconds)
    }
  }

  def parseHttpDate(s: String): Option[Long] =
    Option(DateFormatter.parseHttpDate(unquote(s.trim))).map(_.getTime)
}

private[cache] trait ExpiresSupport {
  import ExpiresSupport._

  def clock: Clock

  /**
   * Compute until when a response is fresh, as defined in RFC 9111 §4.2
   *
   * @param cacheHeaders
   *   the caching header fields of the stored response
   * @param statusCode
   *   the status code of the stored response
   * @param responseHeaders
   *   the received response headers, where Date and Age are taken from
   * @return
   *   the timestamp until which the response is fresh, None if it has to be revalidated
   */
  def freshUntil(cacheHeaders: CacheHeaders, statusCode: Int, responseHeaders: HttpHeaders): Option[Long] =
    if (cacheHeaders.noStore || cacheHeaders.noCache || cacheHeaders.pragmaNoCache) {
      None
    } else if (
      cacheHeaders.maxAge.isEmpty && cacheHeaders.expires.isEmpty && cacheHeaders.lastModified.isEmpty && !ImplicitlyFreshStatusCodes.contains(statusCode)
    ) {
      // nothing to compute freshness from, spare parsing Date
      None
    } else {
      val now = clock.nowMillis
      // RFC 9110 §6.6.1: a recipient that receives a message without a Date has to use its receive time
      val date = Option(responseHeaders.get(HttpHeaderNames.DATE)).flatMap(parseHttpDate).getOrElse(now)

      // RFC 9111 §4.2.2: heuristic freshness when there's no explicit expiration time, restricted like Chrome
      def heuristicLifetime: Option[Long] = {
        val fromLastModified =
          if (HeuristicFreshnessStatusCodes.contains(statusCode) && !cacheHeaders.mustRevalidate) {
            cacheHeaders.lastModified
              .flatMap(parseHttpDate)
              .filter(_ <= date)
              .map(lastModified => (date - lastModified) / HeuristicFraction)
          } else {
            None
          }

        fromLastModified.orElse(Option.when(ImplicitlyFreshStatusCodes.contains(statusCode))(FreshForever))
      }

      // RFC 9111 §4.2.1, but like Chrome, invalid max-age and Expires other than "0" are ignored instead of meaning "already expired"
      val freshnessLifetime: Option[Long] =
        cacheHeaders.maxAge match {
          case Some(maxAge) => Some(maxAge * 1000)
          case _            =>
            cacheHeaders.expires.map(_.trim) match {
              case Some("0") => Some(0L)
              case expires   => expires.flatMap(parseHttpDate).map(_ - date).orElse(heuristicLifetime)
            }
        }

      freshnessLifetime.flatMap {
        case FreshForever => Some(FreshForever)
        case lifetime     =>
          // RFC 9111 §4.2.3, the response delay is negligible as we compute on receipt
          val apparentAge = math.max(0L, now - date)
          val ageValue = Option(responseHeaders.get(HttpHeaderNames.AGE)).flatMap(age => parseDeltaSeconds(age.trim)).fold(0L)(_ * 1000)
          val currentAge = math.max(apparentAge, ageValue)
          val deadline = now + lifetime - currentAge
          Option.when(deadline > now)(deadline)
      }
    }
}
