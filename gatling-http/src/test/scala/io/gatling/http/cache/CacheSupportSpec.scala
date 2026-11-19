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

import io.gatling.commons.util.{ Clock, DefaultClock }
import io.gatling.core.EmptySession
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.http.client.{ Request, RequestBuilder }
import io.gatling.http.client.uri.Uri
import io.gatling.http.engine.tx.HttpTx
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.{ HttpRequest, HttpRequestConfig }

import io.netty.handler.codec.DateFormatter
import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaderNames, HttpHeaders, HttpMethod, HttpResponseStatus }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class CacheSupportSpec extends AnyFlatSpecLike with Matchers with EmptySession {
  private val configuration = GatlingConfiguration.loadForTest()
  private val httpCaches = new HttpCaches(new DefaultClock, configuration)

  private val Now = 1795521600000L // Tue, 24 Nov 2026 12:00:00 GMT
  private val fixedClock: Clock = new Clock {
    override def nowMillis: Long = Now
  }
  private val fixedHttpCaches = new HttpCaches(fixedClock, configuration)

  private def httpDate(millis: Long): String = DateFormatter.format(new ju.Date(millis))

  private def responseHeaders(headers: (CharSequence, String)*): HttpHeaders = {
    val httpHeaders = new DefaultHttpHeaders
    headers.foreach { case (headerName, headerValue) => httpHeaders.add(headerName, headerValue) }
    httpHeaders
  }

  private def freshUntil(headers: (CharSequence, String)*): Option[Long] =
    freshUntilWithStatus(200, headers: _*)

  private def freshUntilWithStatus(status: Int, headers: (CharSequence, String)*): Option[Long] = {
    val httpHeaders = responseHeaders(headers: _*)
    fixedHttpCaches.freshUntil(CacheHeaders(httpHeaders), status, httpHeaders)
  }

  private val Minute = 60 * 1000L
  private val Day = 24 * 60 * Minute

  "freshUntil()" should "honor Pragma: no-cache" in {
    freshUntil(HttpHeaderNames.PRAGMA -> "no-cache") shouldBe None
    freshUntil(HttpHeaderNames.PRAGMA -> "no-cache", HttpHeaderNames.CACHE_CONTROL -> "max-age=3600") shouldBe None
    freshUntil(HttpHeaderNames.PRAGMA -> "no-cache", HttpHeaderNames.EXPIRES -> "3600") shouldBe None
  }

  it should "only honor Pragma no-cache token, case-insensitively, like Chrome" in {
    freshUntil(HttpHeaderNames.PRAGMA -> "foo, No-Cache", HttpHeaderNames.CACHE_CONTROL -> "max-age=60") shouldBe None
    freshUntil(HttpHeaderNames.PRAGMA -> "no-cache-please", HttpHeaderNames.CACHE_CONTROL -> "max-age=60") shouldBe Some(Now + Minute)
  }

  it should "honor Cache-Control max-age" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=1") shouldBe Some(Now + 1000)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "private, max-age=3600, must-revalidate") shouldBe Some(Now + 3600 * 1000)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "MAX-AGE=60") shouldBe Some(Now + Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age= 60") shouldBe Some(Now + Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=00000000060") shouldBe Some(Now + Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=60, max-age=0") shouldBe Some(Now + Minute)
  }

  it should "ignore invalid max-age, like Chrome" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=\"60\"") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age = 60") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=nicolas", HttpHeaderNames.EXPIRES -> httpDate(Now + Day)) shouldBe Some(Now + Day)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=-1", HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Now + Day)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=, max-age=60") shouldBe Some(Now + Minute)
  }

  it should "consider responses with no-cache, no-store or invalid max-age as stale" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "public, no-cache") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "no-store") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "public, max-age=0") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "public, max-age=-1") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=nicolas") shouldBe None
  }

  it should "only consider exact no-cache and no-store, like Chrome" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "no-cache=\"Set-Cookie\", max-age=60") shouldBe Some(Now + Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "no-cache=\"Set-Cookie, Set-Cookie2\", max-age=60") shouldBe Some(Now + Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "no-store=\"foo\", max-age=60") shouldBe Some(Now + Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "private=\"a, no-store, b\", max-age=60") shouldBe Some(Now + Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "NO-STORE, max-age=60") shouldBe None
  }

  it should "combine multiple Cache-Control field lines" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "public", HttpHeaderNames.CACHE_CONTROL -> "max-age=60") shouldBe Some(Now + Minute)
  }

  it should "cap max-age to 2^31 seconds" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=99999999999999999999") shouldBe Some(Now + 2147483648L * 1000)
  }

  it should "compute Expires freshness relative to Date" in {
    freshUntil(HttpHeaderNames.DATE -> httpDate(Now), HttpHeaderNames.EXPIRES -> httpDate(Now + Day)) shouldBe Some(Now + Day)
    // origin clock is 1 hour ahead
    freshUntil(HttpHeaderNames.DATE -> httpDate(Now + 60 * Minute), HttpHeaderNames.EXPIRES -> httpDate(Now + 90 * Minute)) shouldBe Some(Now + 30 * Minute)
  }

  it should "consider invalid or past Expires as stale" in {
    freshUntil(HttpHeaderNames.EXPIRES -> "0") shouldBe None
    freshUntil(HttpHeaderNames.EXPIRES -> httpDate(Now - Day)) shouldBe None
    // no heuristic freshness when there's an explicit expiration time
    freshUntil(HttpHeaderNames.EXPIRES -> "0", HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe None
    freshUntil(HttpHeaderNames.EXPIRES -> httpDate(Now - Day), HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe None
  }

  it should "ignore invalid Expires other than 0, like Chrome" in {
    freshUntil(HttpHeaderNames.EXPIRES -> "-1") shouldBe None
    freshUntil(HttpHeaderNames.EXPIRES -> "-1", HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Now + Day)
    freshUntil(HttpHeaderNames.EXPIRES -> "garbage", HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Now + Day)
    freshUntilWithStatus(410, HttpHeaderNames.EXPIRES -> "-1") shouldBe Some(Long.MaxValue)
  }

  it should "honor Expires when Cache-Control has no max-age" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "public", HttpHeaderNames.EXPIRES -> httpDate(Now + Day)) shouldBe Some(Now + Day)
  }

  it should "give priority to max-age over Expires" in {
    freshUntil(HttpHeaderNames.EXPIRES -> httpDate(Now + Day), HttpHeaderNames.CACHE_CONTROL -> "no-store") shouldBe None
    freshUntil(HttpHeaderNames.EXPIRES -> httpDate(Now + Day), HttpHeaderNames.CACHE_CONTROL -> "max-age=0") shouldBe None
    freshUntil(HttpHeaderNames.EXPIRES -> httpDate(Now - Day), HttpHeaderNames.CACHE_CONTROL -> "max-age=60") shouldBe Some(Now + Minute)
  }

  it should "subtract the current age" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=3600", HttpHeaderNames.AGE -> "3000") shouldBe Some(Now + 10 * Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=3600", HttpHeaderNames.DATE -> httpDate(Now - 10 * Minute)) shouldBe Some(Now + 50 * Minute)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=3600", HttpHeaderNames.AGE -> "3600") shouldBe None
  }

  it should "compute heuristic freshness from Last-Modified" in {
    freshUntil(HttpHeaderNames.DATE -> httpDate(Now), HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Now + Day)
    // no Date: use receive time
    freshUntil(HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Now + Day)
    freshUntil(HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.ETAG -> "\"foo\"") shouldBe Some(Now + Day)
    freshUntilWithStatus(203, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Now + Day)
    freshUntilWithStatus(206, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Now + Day)
  }

  it should "not compute heuristic freshness without Last-Modified" in {
    freshUntil(HttpHeaderNames.ETAG -> "\"foo\"") shouldBe None
  }

  it should "not compute heuristic freshness when forbidden" in {
    freshUntil(HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.CACHE_CONTROL -> "no-cache") shouldBe None
    freshUntil(HttpHeaderNames.LAST_MODIFIED -> httpDate(Now + Day)) shouldBe None
    // like Chrome
    freshUntil(HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.CACHE_CONTROL -> "must-revalidate") shouldBe None
  }

  it should "only compute heuristic freshness for 200, 203 and 206, like Chrome" in {
    freshUntilWithStatus(204, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe None
    freshUntilWithStatus(302, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe None
    freshUntilWithStatus(404, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe None
    freshUntilWithStatus(500, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe None
    freshUntilWithStatus(302, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.CACHE_CONTROL -> "public") shouldBe None
  }

  it should "consider 300, 301, 308 and 410 fresh forever without an explicit expiration time, like Chrome" in {
    Seq(300, 301, 308, 410).foreach { status =>
      freshUntilWithStatus(status) shouldBe Some(Long.MaxValue)
      freshUntilWithStatus(status, HttpHeaderNames.CACHE_CONTROL -> "must-revalidate", HttpHeaderNames.AGE -> "3600") shouldBe Some(Long.MaxValue)
      freshUntilWithStatus(status, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Long.MaxValue)
      freshUntilWithStatus(status, HttpHeaderNames.CACHE_CONTROL -> "max-age=60") shouldBe Some(Now + Minute)
      freshUntilWithStatus(status, HttpHeaderNames.CACHE_CONTROL -> "max-age=0") shouldBe None
      freshUntilWithStatus(status, HttpHeaderNames.EXPIRES -> "0") shouldBe None
      freshUntilWithStatus(status, HttpHeaderNames.CACHE_CONTROL -> "no-cache") shouldBe None
    }
  }

  it should "honor explicit freshness whatever the status code, like Chrome" in {
    freshUntilWithStatus(500, HttpHeaderNames.CACHE_CONTROL -> "max-age=60") shouldBe Some(Now + Minute)
    freshUntilWithStatus(418, HttpHeaderNames.EXPIRES -> httpDate(Now + Day)) shouldBe Some(Now + Day)
  }

  "cacheContent()" should "store a response with only Last-Modified as fresh" in new ContentContext {
    store(200, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day))
    entry.flatMap(_.freshUntil) shouldBe Some(Now + Day)
    isFresh shouldBe true
  }

  it should "store a response with only ETag for revalidation" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"foo\"")
    entry.flatMap(_.etag) shouldBe Some("\"foo\"")
    isFresh shouldBe false
  }

  it should "not store a response with no-store, and remove the stored one" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"foo\"")
    store(200, HttpHeaderNames.ETAG -> "\"bar\"", HttpHeaderNames.CACHE_CONTROL -> "no-store")
    entry shouldBe None
  }

  it should "remove the stored response when the new one can't be reused" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"foo\"")
    store(200)
    entry shouldBe None
  }

  it should "only store responses for revalidation when their status code is 200 or 206, like Chrome" in new ContentContext {
    store(500, HttpHeaderNames.ETAG -> "\"foo\"")
    entry shouldBe None
    store(404, HttpHeaderNames.ETAG -> "\"foo\"")
    entry shouldBe None
    store(203, HttpHeaderNames.ETAG -> "\"foo\"")
    entry shouldBe None
    store(206, HttpHeaderNames.ETAG -> "\"foo\"")
    entry.flatMap(_.etag) shouldBe Some("\"foo\"")
  }

  it should "not send validators for responses that aren't 200 or 206, like Chrome" in new ContentContext {
    store(404, HttpHeaderNames.ETAG -> "\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    isFresh shouldBe true
    entry.flatMap(_.etag) shouldBe None
  }

  it should "store a response with an unknown status code and explicit freshness, like Chrome" in new ContentContext {
    store(418, HttpHeaderNames.EXPIRES -> httpDate(Now + Day))
    isFresh shouldBe true
  }

  it should "store a 410 response with no caching headers as fresh forever, like Chrome" in new ContentContext {
    store(410)
    isFresh shouldBe true
  }

  it should "not store responses to POST requests" in new ContentContext {
    override val method: HttpMethod = HttpMethod.POST
    store(200, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    entry shouldBe None
  }

  it should "freshen the stored response on 304" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"foo\"", HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.CACHE_CONTROL -> "max-age=0")
    isFresh shouldBe false

    store(304, HttpHeaderNames.ETAG -> "\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    entry.flatMap(_.freshUntil) shouldBe Some(Now + Minute)
    entry.flatMap(_.lastModified) shouldBe Some(httpDate(Now - 10 * Day))
  }

  it should "keep heuristic freshness of the stored response on 304" in new ContentContext {
    store(200, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day))
    store(304, HttpHeaderNames.DATE -> httpDate(Now))
    entry.flatMap(_.freshUntil) shouldBe Some(Now + Day)
    entry.flatMap(_.lastModified) shouldBe Some(httpDate(Now - 10 * Day))
  }

  it should "keep the explicit freshness of the stored response on 304" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    store(304, HttpHeaderNames.ETAG -> "\"foo\"")
    entry.flatMap(_.freshUntil) shouldBe Some(Now + Minute)
  }

  it should "not store a 304 when no response is stored" in new ContentContext {
    store(304, HttpHeaderNames.ETAG -> "\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    entry shouldBe None
  }

  it should "not update the stored response with a 304 with a different ETag" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=0")
    store(304, HttpHeaderNames.ETAG -> "\"bar\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    entry.flatMap(_.etag) shouldBe Some("\"foo\"")
    isFresh shouldBe false
  }

  it should "not update a stored response with a weak ETag with a 304 with a strong ETag" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "W/\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=0")
    store(304, HttpHeaderNames.ETAG -> "\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    isFresh shouldBe false
  }

  it should "update the stored response with a 304 with a matching weak ETag" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=0")
    store(304, HttpHeaderNames.ETAG -> "W/\"foo\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    entry.flatMap(_.freshUntil) shouldBe Some(Now + Minute)
  }

  it should "not update the stored response with a 304 with a different Last-Modified" in new ContentContext {
    store(200, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.CACHE_CONTROL -> "max-age=0")
    store(304, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - Day), HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    entry.flatMap(_.lastModified) shouldBe Some(httpDate(Now - 10 * Day))
    isFresh shouldBe false
  }

  it should "not store a response with Vary: *, and remove the stored one" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"foo\"")
    store(200, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600", HttpHeaderNames.VARY -> "Accept-Encoding, *")
    entry shouldBe None
  }

  it should "store a response with a Vary other than *" in new ContentContext {
    store(200, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600", HttpHeaderNames.VARY -> "Accept-Encoding")
    isFresh shouldBe true
  }

  it should "invalidate the stored response on a non-error response to an unsafe method" in new ContentContext {
    store(200, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    respond(HttpMethod.HEAD, 200, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    respond(HttpMethod.POST, 303)
    entry shouldBe None
    fixedHttpCaches.isFresh(session, new RequestBuilder("request", HttpMethod.HEAD, Uri.create("http://example.com/app.js"), null).build()) shouldBe false
  }

  it should "not invalidate the stored response on an error response to an unsafe method" in new ContentContext {
    store(200, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    respond(HttpMethod.DELETE, 500)
    isFresh shouldBe true
  }

  it should "not invalidate the stored response on a response to a safe method" in new ContentContext {
    store(200, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    respond(HttpMethod.OPTIONS, 200)
    isFresh shouldBe true
  }

  private val LastModified = HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)

  it should "only add validators when the stored response is stale" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    validatorsAddedTo(requestWith()) shouldBe (None -> None)

    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=0")
    validatorsAddedTo(requestWith()) shouldBe (Some("\"v1\"") -> Some(httpDate(Now - 10 * Day)))
  }

  "request Cache-Control: no-cache or Pragma: no-cache" should "neither reuse nor revalidate the stored response, but store the new one, like Chrome" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")

    Seq(HttpHeaderNames.CACHE_CONTROL -> "no-cache", HttpHeaderNames.PRAGMA -> "No-Cache", HttpHeaderNames.CACHE_CONTROL -> "foo, no-cache").foreach { header =>
      isFreshFor(requestWith(header)) shouldBe false
      validatorsAddedTo(requestWith(header)) shouldBe (None -> None)
    }

    storeWith(requestWith(HttpHeaderNames.CACHE_CONTROL -> "no-cache"), 200, HttpHeaderNames.ETAG -> "\"v2\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    entry.flatMap(_.etag) shouldBe Some("\"v2\"")
    isFresh shouldBe true
  }

  "request Cache-Control: max-age=0" should "revalidate the stored response, like Chrome" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    val request: Request = requestWith(HttpHeaderNames.CACHE_CONTROL -> "max-age=0")
    isFreshFor(request) shouldBe false
    validatorsAddedTo(request) shouldBe (Some("\"v1\"") -> Some(httpDate(Now - 10 * Day)))

    storeWith(request, 304, HttpHeaderNames.ETAG -> "\"v1\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=60")
    entry.flatMap(_.freshUntil) shouldBe Some(Now + Minute)
  }

  "user validators" should "never reuse the stored response, nor be overridden or completed, like Chrome" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=0")
    val request: Request = requestWith(HttpHeaderNames.IF_NONE_MATCH -> "\"bar\"")
    validatorsAddedTo(request) shouldBe (Some("\"bar\"") -> None)

    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    isFreshFor(requestWith(HttpHeaderNames.IF_NONE_MATCH -> "\"v1\"")) shouldBe false
    isFreshFor(requestWith(HttpHeaderNames.IF_MODIFIED_SINCE -> httpDate(Now - 10 * Day))) shouldBe false
  }

  it should "update the stored response when they identify it, like Chrome" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    storeWith(
      requestWith(HttpHeaderNames.IF_NONE_MATCH -> "\"v1\""),
      200,
      HttpHeaderNames.ETAG -> "\"v2\"",
      LastModified,
      HttpHeaderNames.CACHE_CONTROL -> "max-age=3600"
    )
    entry.flatMap(_.etag) shouldBe Some("\"v2\"")

    storeWith(requestWith(HttpHeaderNames.IF_MODIFIED_SINCE -> httpDate(Now - 10 * Day)), 200, HttpHeaderNames.ETAG -> "\"v3\"")
    entry.flatMap(_.etag) shouldBe Some("\"v3\"")
  }

  it should "leave the cache untouched when they don't identify the stored response, like Chrome" in new ContentContext {
    storeWith(requestWith(HttpHeaderNames.IF_NONE_MATCH -> "\"x\""), 200, HttpHeaderNames.ETAG -> "\"v1\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    entry shouldBe None

    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    storeWith(requestWith(HttpHeaderNames.IF_NONE_MATCH -> "\"bar\""), 200, HttpHeaderNames.ETAG -> "\"v2\"", HttpHeaderNames.CACHE_CONTROL -> "no-store")
    storeWith(requestWith(HttpHeaderNames.IF_MODIFIED_SINCE -> httpDate(Now)), 200, HttpHeaderNames.ETAG -> "\"v3\"")
    entry.flatMap(_.etag) shouldBe Some("\"v1\"")
    isFresh shouldBe true
  }

  "request If-Match, If-Unmodified-Since and If-Range" should "bypass the cache, like Chrome" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"v1\"", LastModified, HttpHeaderNames.CACHE_CONTROL -> "max-age=0")

    Seq(
      HttpHeaderNames.IF_MATCH -> "\"v1\"",
      HttpHeaderNames.IF_UNMODIFIED_SINCE -> httpDate(Now - 10 * Day),
      HttpHeaderNames.IF_RANGE -> "\"v1\""
    ).foreach { header =>
      validatorsAddedTo(requestWith(header)) shouldBe (None -> None)
      storeWith(requestWith(header), 200, HttpHeaderNames.ETAG -> "\"v2\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
      entry.flatMap(_.etag) shouldBe Some("\"v1\"")
    }

    store(200, HttpHeaderNames.ETAG -> "\"v1\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    isFreshFor(requestWith(HttpHeaderNames.IF_MATCH -> "\"v1\"")) shouldBe false
  }

  "request Range and Cache-Control: no-store" should "still reuse the stored response, like Chrome" in new ContentContext {
    store(200, HttpHeaderNames.ETAG -> "\"v1\"", HttpHeaderNames.CACHE_CONTROL -> "max-age=3600")
    isFreshFor(requestWith(HttpHeaderNames.RANGE -> "bytes=0-1")) shouldBe true
    isFreshFor(requestWith(HttpHeaderNames.CACHE_CONTROL -> "no-store")) shouldBe true
  }

  class ContentContext {
    val method: HttpMethod = HttpMethod.GET
    private val protocol = HttpProtocol(configuration)
    def requestFor(method: HttpMethod, headers: (CharSequence, String)*): Request = {
      val requestBuilder = new RequestBuilder("request", method, Uri.create("http://example.com/app.js"), null)
      headers.foreach { case (name, value) => requestBuilder.addHeader(name, value) }
      requestBuilder.build()
    }

    def requestWith(headers: (CharSequence, String)*): Request = requestFor(HttpMethod.GET, headers: _*)
    private lazy val request = requestFor(method)
    var session: Session = emptySession

    def store(status: Int, headers: (CharSequence, String)*): Unit =
      storeWith(request, status, headers: _*)

    def respond(method: HttpMethod, status: Int, headers: (CharSequence, String)*): Unit =
      storeWith(requestFor(method), status, headers: _*)

    def storeWith(request: Request, status: Int, headers: (CharSequence, String)*): Unit =
      session = fixedHttpCaches.cacheContent(session, protocol, request, HttpResponseStatus.valueOf(status), responseHeaders(headers: _*))

    def entry: Option[ContentCacheEntry] = fixedHttpCaches.contentCacheEntry(session, request)

    def isFresh: Boolean = fixedHttpCaches.isFresh(session, request)

    def isFreshFor(request: Request): Boolean = fixedHttpCaches.isFresh(session, request)

    def validatorsAddedTo(request: Request): (Option[String], Option[String]) = {
      fixedHttpCaches.addValidators(session, request)
      (Option(request.getHeaders.get(HttpHeaderNames.IF_NONE_MATCH)), Option(request.getHeaders.get(HttpHeaderNames.IF_MODIFIED_SINCE)))
    }
  }

  class RedirectContext {
    var session: Session = emptySession

    def addRedirect(from: String, to: String): Unit = {
      val request = new RequestBuilder("request", HttpMethod.GET, Uri.create(from), null)
        .build()
      session = httpCaches.addRedirect(session, request, Uri.create(to))
    }
  }

  "redirect memoization" should "return transaction with no redirect cache" in new RedirectContext {
    val tx = txTo("http://example.com/", session, redirectCount = 0, cache = true)
    val actualTx = httpCaches.applyPermanentRedirect(tx)

    actualTx shouldBe tx
  }

  it should "return updated transaction with single redirect" in new RedirectContext {
    addRedirect("http://example.com/", "http://gatling.io/")

    val origTx = txTo("http://example.com/", session, redirectCount = 0, cache = true)
    val tx = httpCaches.applyPermanentRedirect(origTx)

    tx.request.clientRequest.getUri shouldBe Uri.create("http://gatling.io/")
    tx.redirectCount shouldBe 1
  }

  it should "return updated transaction with several redirects" in new RedirectContext {
    addRedirect("http://example.com/", "http://gatling.io/")
    addRedirect("http://gatling.io/", "http://gatling2.io/")
    addRedirect("http://gatling2.io/", "http://gatling3.io/")

    val origTx = txTo("http://example.com/", session, redirectCount = 0, cache = true)
    val tx = httpCaches.applyPermanentRedirect(origTx)

    tx.request.clientRequest.getUri shouldBe Uri.create("http://gatling3.io/")
    tx.redirectCount shouldBe 3
  }

  it should "return updated transaction with several redirects, with redirectCount preset" in new RedirectContext {
    addRedirect("http://example.com/", "http://gatling.io/")
    addRedirect("http://gatling.io/", "http://gatling2.io/")
    addRedirect("http://gatling2.io/", "http://gatling3.io/")

    // Redirect count is already 2
    val origTx = txTo("http://example.com/", session, 2, cache = true)
    val tx = httpCaches.applyPermanentRedirect(origTx)

    tx.request.clientRequest.getUri shouldBe Uri.create("http://gatling3.io/")
    // After 3 more redirects it is now equal to 5
    tx.redirectCount shouldBe 5
  }

  private def txTo(uri: String, session: Session, redirectCount: Int, cache: Boolean) = {
    val protocol = HttpProtocol(configuration)
    val request = new Request(
      null,
      null,
      Uri.create(uri),
      new DefaultHttpHeaders,
      ju.Collections.emptyList(),
      null,
      0L,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      false,
      null,
      null,
      null
    )

    HttpTx(
      session,
      request = HttpRequest(
        requestName = "mockHttpTx",
        clientRequest = request,
        requestConfig = HttpRequestConfig(
          checks = Nil,
          postChecks = Nil,
          responseTransformer = None,
          throttled = false,
          silent = None,
          followRedirect = true,
          checksumAlgorithms = Nil,
          storeBodyParts = false,
          defaultCharset = configuration.core.charset,
          httpProtocol = protocol,
          explicitResources = Nil
        )
      ),
      next = null,
      resourceTx = None,
      redirectCount = redirectCount
    )
  }
}
