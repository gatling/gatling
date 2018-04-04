/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.http.HeaderNames
import io.gatling.http.client.Request
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.response.Response

object ContentCacheKey {
  def apply(request: Request): ContentCacheKey =
    new ContentCacheKey(request.getUri, request.getMethod.name, Cookies(request.getCookies))
}

case class ContentCacheKey(uri: Uri, method: String, cookies: Map[String, String])

case class ContentCacheEntry(expires: Option[Long], etag: Option[String], lastModified: Option[String])

object HttpContentCacheSupport {
  val HttpContentCacheAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.contentCache"
}

trait HttpContentCacheSupport extends ExpiresSupport {

  import HttpContentCacheSupport._

  def configuration: GatlingConfiguration

  val httpContentCacheHandler = new SessionCacheHandler[ContentCacheKey, ContentCacheEntry](HttpContentCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def cacheContent(httpProtocol: HttpProtocol, request: Request, response: Response): Session => Session =
    if (httpProtocol.requestPart.cache) {

      val expires = getResponseExpires(response)
      val etag = response.header(HeaderNames.ETag)
      val lastModified = response.header(HeaderNames.LastModified)

      if (expires.isDefined || etag.isDefined || lastModified.isDefined) {
        val key = ContentCacheKey(request)
        val value = ContentCacheEntry(expires, etag, lastModified)
        httpContentCacheHandler.addEntry(_, key, value)
      } else
        Session.Identity
    } else
      Session.Identity

  def contentCacheEntry(session: Session, request: Request): Option[ContentCacheEntry] =
    httpContentCacheHandler.getEntry(session, ContentCacheKey(request))

  def clearContentCache(session: Session, request: Request): Session =
    httpContentCacheHandler.removeEntry(session, ContentCacheKey(request))
}
