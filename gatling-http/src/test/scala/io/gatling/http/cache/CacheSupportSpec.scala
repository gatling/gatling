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
    fixedHttpCaches.freshUntil(CacheHeaders(httpHeaders), ExpiresSupport.isHeuristicallyCacheable(HttpResponseStatus.valueOf(status)), httpHeaders)
  }

  private val Minute = 60 * 1000L
  private val Day = 24 * 60 * Minute

  "freshUntil()" should "honor Pragma: no-cache" in {
    freshUntil(HttpHeaderNames.PRAGMA -> "no-cache") shouldBe None
    freshUntil(HttpHeaderNames.PRAGMA -> "no-cache", HttpHeaderNames.CACHE_CONTROL -> "max-age=3600") shouldBe None
    freshUntil(HttpHeaderNames.PRAGMA -> "no-cache", HttpHeaderNames.EXPIRES -> "3600") shouldBe None
  }

  it should "honor Cache-Control max-age" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=1") shouldBe Some(Now + 1000)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "private, max-age=3600, must-revalidate") shouldBe Some(Now + 3600 * 1000)
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "Max-Age=\"60\"") shouldBe Some(Now + Minute)
  }

  it should "consider responses with no-cache, no-store or invalid max-age as stale" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "public, no-cache") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "no-store") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "public, max-age=0") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "public, max-age=-1") shouldBe None
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "max-age=nicolas") shouldBe None
  }

  it should "only consider unqualified no-cache" in {
    freshUntil(HttpHeaderNames.CACHE_CONTROL -> "no-cache=\"Set-Cookie\", max-age=60") shouldBe Some(Now + Minute)
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
    freshUntil(HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.CACHE_CONTROL -> "must-revalidate") shouldBe Some(Now + Day)
    freshUntilWithStatus(404, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe Some(Now + Day)
  }

  it should "not compute heuristic freshness without Last-Modified" in {
    freshUntil(HttpHeaderNames.ETAG -> "\"foo\"") shouldBe None
  }

  it should "not compute heuristic freshness when forbidden" in {
    freshUntil(HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.CACHE_CONTROL -> "no-cache") shouldBe None
    freshUntil(HttpHeaderNames.LAST_MODIFIED -> httpDate(Now + Day)) shouldBe None
    freshUntilWithStatus(302, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe None
    freshUntilWithStatus(500, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day)) shouldBe None
  }

  it should "compute heuristic freshness for non heuristically cacheable status codes when public" in {
    freshUntilWithStatus(302, HttpHeaderNames.LAST_MODIFIED -> httpDate(Now - 10 * Day), HttpHeaderNames.CACHE_CONTROL -> "public") shouldBe Some(Now + Day)
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

  it should "not store a response with a status code that isn't heuristically cacheable and no explicit freshness" in new ContentContext {
    store(500, HttpHeaderNames.ETAG -> "\"foo\"")
    entry shouldBe None
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

  class ContentContext {
    val method: HttpMethod = HttpMethod.GET
    private val protocol = HttpProtocol(configuration)
    private lazy val request = new RequestBuilder("request", method, Uri.create("http://example.com/app.js"), null).build()
    var session: Session = emptySession

    def store(status: Int, headers: (CharSequence, String)*): Unit =
      session = fixedHttpCaches.cacheContent(session, protocol, request, HttpResponseStatus.valueOf(status), responseHeaders(headers: _*))

    def entry: Option[ContentCacheEntry] = fixedHttpCaches.contentCacheEntry(session, request)

    def isFresh: Boolean = fixedHttpCaches.isFresh(session, request)
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
