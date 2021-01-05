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

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.cache.SessionCacheHandler
import io.gatling.http.client.{ Request, RequestBuilder }
import io.gatling.http.client.uri.Uri
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.engine.tx.HttpTx

import io.netty.handler.codec.http.HttpHeaderNames

private[cache] object PermanentRedirectCacheKey {
  def apply(request: Request): PermanentRedirectCacheKey =
    new PermanentRedirectCacheKey(request.getUri, Cookies(request.getCookies))
}

private[cache] final case class PermanentRedirectCacheKey(uri: Uri, cookies: Cookies)

private[cache] object PermanentRedirectCacheSupport {
  val HttpPermanentRedirectCacheAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.redirects"
}

private[cache] trait PermanentRedirectCacheSupport {

  import PermanentRedirectCacheSupport._

  def configuration: GatlingConfiguration

  private[this] val httpPermanentRedirectCacheHandler =
    new SessionCacheHandler[PermanentRedirectCacheKey, Uri](HttpPermanentRedirectCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def addRedirect(session: Session, from: Request, to: Uri): Session =
    if (httpPermanentRedirectCacheHandler.enabled) {
      httpPermanentRedirectCacheHandler.addEntry(session, PermanentRedirectCacheKey(from), to)
    } else {
      session
    }

  private[this] def permanentRedirect(session: Session, request: Request, maxRedirects: Int): Option[(Uri, Int)] = {

    @tailrec
    def permanentRedirectRec(from: PermanentRedirectCacheKey, redirectCount: Int): Option[(Uri, Int)] =
      httpPermanentRedirectCacheHandler.getEntry(session, from) match {
        case Some(toUri) if redirectCount < maxRedirects => permanentRedirectRec(new PermanentRedirectCacheKey(toUri, from.cookies), redirectCount + 1)

        case _ =>
          redirectCount match {
            case 0 => None
            case _ => Some((from.uri, redirectCount))
          }
      }

    permanentRedirectRec(PermanentRedirectCacheKey(request), redirectCount = 0)
  }

  private[this] def redirectRequest(request: Request, toUri: Uri, session: Session): Request = {
    request.getHeaders.remove(HttpHeaderNames.COOKIE)
    val cookies = CookieSupport.getStoredCookies(session, toUri)
    new RequestBuilder(request, toUri)
      .setDefaultCharset(configuration.core.charset)
      .setCookies(cookies.asJava)
      .build
  }

  def applyPermanentRedirect(origTx: HttpTx): HttpTx =
    if (origTx.request.requestConfig.httpProtocol.requestPart.cache && httpPermanentRedirectCacheHandler.enabled) {
      permanentRedirect(origTx.session, origTx.request.clientRequest, origTx.request.requestConfig.httpProtocol.responsePart.maxRedirects) match {
        case Some((targetUri, redirectCount)) =>
          val newClientRequest = redirectRequest(origTx.request.clientRequest, targetUri, origTx.session)

          origTx.copy(
            request = origTx.request.copy(clientRequest = newClientRequest),
            redirectCount = origTx.redirectCount + redirectCount
          )

        case _ => origTx
      }
    } else {
      origTx
    }
}
