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

package io.gatling.http.cache

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.cache.SessionCacheHandler
import io.gatling.http.client.Request
import io.gatling.http.client.uri.Uri
import io.gatling.http.protocol.HttpProtocol

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaders }

object ContentCacheKey {
  def apply(request: Request): ContentCacheKey =
    new ContentCacheKey(request.getUri, request.getMethod.name, Cookies(request.getCookies))
}

private[cache] final case class ContentCacheKey(uri: Uri, method: String, cookies: Map[String, String])

private[http] final case class ContentCacheEntry(expires: Option[Long], etag: Option[String], lastModified: Option[String])

private[cache] object HttpContentCacheSupport {
  val HttpContentCacheAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.contentCache"
}

private[cache] trait HttpContentCacheSupport extends ExpiresSupport {

  import HttpContentCacheSupport._

  def configuration: GatlingConfiguration

  private[this] val httpContentCacheHandler =
    new SessionCacheHandler[ContentCacheKey, ContentCacheEntry](HttpContentCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def cacheContent(session: Session, httpProtocol: HttpProtocol, request: Request, responseHeaders: HttpHeaders): Session =
    if (httpProtocol.requestPart.cache && httpContentCacheHandler.enabled) {
      val expires = getResponseExpires(responseHeaders)
      val etag = Option(responseHeaders.get(HttpHeaderNames.ETAG))
      val lastModified = Option(responseHeaders.get(HttpHeaderNames.LAST_MODIFIED))

      if (expires.isDefined || etag.isDefined || lastModified.isDefined) {
        val key = ContentCacheKey(request)
        val value = ContentCacheEntry(expires, etag, lastModified)
        httpContentCacheHandler.addEntry(session, key, value)
      } else {
        session
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

  def clearContentCache(session: Session, request: Request): Session =
    if (httpContentCacheHandler.enabled) {
      httpContentCacheHandler.removeEntry(session, ContentCacheKey(request))
    } else {
      session
    }
}
