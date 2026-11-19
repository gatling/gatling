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

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.commons.util.Clock

import io.netty.handler.codec.DateFormatter
import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaderValues, HttpHeaders, HttpResponseStatus }

/**
 * The header fields of a stored response that drive caching decisions (RFC 9111).
 */
private[http] final case class CacheHeaders(
    cacheControl: Option[String],
    expires: Option[String],
    pragma: Option[String],
    etag: Option[String],
    lastModified: Option[String]
) {
  private[cache] lazy val directives: Map[String, Option[String]] = cacheControl.fold(Map.empty[String, Option[String]])(ExpiresSupport.parseCacheControl)

  // only the unqualified form, the qualified one only forbids reusing the listed fields, RFC 9111 §5.2.2.4
  private[cache] def noCache: Boolean = directives.get("no-cache").exists(_.isEmpty)

  private[cache] def noStore: Boolean = directives.contains("no-store")

  // RFC 9111 §5.4 deprecates Pragma, but browsers still honor it on responses
  private[cache] def pragmaNoCache: Boolean = pragma.exists(_.contains(HttpHeaderValues.NO_CACHE.toString))

  // RFC 9111 §3
  private[cache] def allowsStorage: Boolean =
    directives.contains("max-age") || directives.contains("public") || directives.contains("private") || expires.isDefined

  /**
   * Update the header fields of a stored response with the ones of a 304 response, RFC 9111 §4.3.4.
   */
  def updatedWith(notModified: CacheHeaders): CacheHeaders =
    CacheHeaders(
      cacheControl = notModified.cacheControl.orElse(cacheControl),
      expires = notModified.expires.orElse(expires),
      pragma = notModified.pragma.orElse(pragma),
      etag = notModified.etag.orElse(etag),
      lastModified = notModified.lastModified.orElse(lastModified)
    )
}

private[http] object CacheHeaders {
  def apply(headers: HttpHeaders): CacheHeaders = {
    // multiple Cache-Control field lines are combined into a single comma-separated list, RFC 9110 §5.3
    val cacheControls = headers.getAll(HttpHeaderNames.CACHE_CONTROL)
    new CacheHeaders(
      cacheControl = if (cacheControls.isEmpty) None else Some(cacheControls.asScala.mkString(",")),
      expires = Option(headers.get(HttpHeaderNames.EXPIRES)),
      pragma = Option(headers.get(HttpHeaderNames.PRAGMA)),
      etag = Option(headers.get(HttpHeaderNames.ETAG)),
      lastModified = Option(headers.get(HttpHeaderNames.LAST_MODIFIED))
    )
  }
}

private[cache] object ExpiresSupport {
  // RFC 9111 §1.2.2
  private val MaxDeltaSeconds = 2147483648L

  // RFC 9110 §15.1
  private val HeuristicallyCacheableStatusCodes: Set[HttpResponseStatus] = Set(
    HttpResponseStatus.OK,
    HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION,
    HttpResponseStatus.NO_CONTENT,
    HttpResponseStatus.PARTIAL_CONTENT,
    HttpResponseStatus.MULTIPLE_CHOICES,
    HttpResponseStatus.MOVED_PERMANENTLY,
    HttpResponseStatus.PERMANENT_REDIRECT,
    HttpResponseStatus.NOT_FOUND,
    HttpResponseStatus.METHOD_NOT_ALLOWED,
    HttpResponseStatus.GONE,
    HttpResponseStatus.REQUEST_URI_TOO_LONG,
    HttpResponseStatus.NOT_IMPLEMENTED
  )

  // RFC 9111 §4.2.2: "if the response has a Last-Modified header field, caches are encouraged to use a heuristic expiration value
  // that is no more than some fraction of the interval since that time. A typical setting of this fraction might be 10%."
  private val HeuristicFraction = 10

  def isHeuristicallyCacheable(status: HttpResponseStatus): Boolean = HeuristicallyCacheableStatusCodes.contains(status)

  private def unquote(s: String): String =
    if (s.length >= 2 && s.charAt(0) == '"' && s.charAt(s.length - 1) == '"') s.substring(1, s.length - 1) else s

  def parseCacheControl(value: String): Map[String, Option[String]] =
    value
      .split(',')
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { directive =>
        directive.indexOf('=') match {
          case -1 => directive.toLowerCase(ju.Locale.ROOT) -> None
          case i  => directive.substring(0, i).trim.toLowerCase(ju.Locale.ROOT) -> Some(unquote(directive.substring(i + 1).trim))
        }
      }
      // RFC 9111 §4.2.1: when a directive is repeated, use the first occurrence
      .foldLeft(Map.empty[String, Option[String]]) { case (acc, (name, value)) => if (acc.contains(name)) acc else acc.updated(name, value) }

  // RFC 9111 §1.2.2
  def parseDeltaSeconds(s: String): Option[Long] =
    if (s.nonEmpty && s.forall(c => c >= '0' && c <= '9')) {
      Some(if (s.length > 10) MaxDeltaSeconds else math.min(s.toLong, MaxDeltaSeconds))
    } else {
      None
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
   * @param heuristicallyCacheable
   *   if the status code of the stored response allows for heuristic freshness
   * @param responseHeaders
   *   the received response headers, where Date and Age are taken from
   * @return
   *   the timestamp until which the response is fresh, None if it has to be revalidated
   */
  def freshUntil(cacheHeaders: CacheHeaders, heuristicallyCacheable: Boolean, responseHeaders: HttpHeaders): Option[Long] =
    if (cacheHeaders.noStore || cacheHeaders.noCache || cacheHeaders.pragmaNoCache) {
      None
    } else {
      val now = clock.nowMillis
      // RFC 9110 §6.6.1: a recipient that receives a message without a Date has to use its receive time
      val date = Option(responseHeaders.get(HttpHeaderNames.DATE)).flatMap(parseHttpDate).getOrElse(now)

      // RFC 9111 §4.2.1
      val freshnessLifetime: Option[Long] =
        cacheHeaders.directives.get("max-age") match {
          case Some(maxAge) =>
            // invalid max-age makes the response stale
            maxAge.flatMap(parseDeltaSeconds).map(_ * 1000)
          case _ =>
            cacheHeaders.expires match {
              case Some(expires) =>
                // invalid dates, especially "0", mean "already expired"
                parseHttpDate(expires).map(_ - date)
              case _ =>
                // RFC 9111 §4.2.2: heuristic freshness when there's no explicit expiration time
                if (heuristicallyCacheable || cacheHeaders.directives.contains("public")) {
                  cacheHeaders.lastModified
                    .flatMap(parseHttpDate)
                    .filter(_ <= date)
                    .map(lastModified => (date - lastModified) / HeuristicFraction)
                } else {
                  None
                }
            }
        }

      freshnessLifetime.flatMap { lifetime =>
        // RFC 9111 §4.2.3, the response delay is negligible as we compute on receipt
        val apparentAge = math.max(0L, now - date)
        val ageValue = Option(responseHeaders.get(HttpHeaderNames.AGE)).flatMap(age => parseDeltaSeconds(age.trim)).fold(0L)(_ * 1000)
        val currentAge = math.max(apparentAge, ageValue)
        val deadline = now + lifetime - currentAge
        Option.when(deadline > now)(deadline)
      }
    }
}
