/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache.SessionCacheHandler
import io.gatling.core.session.{ SessionPrivateAttributes, Session }
import io.gatling.http.action.sync.HttpTx

import org.asynchttpclient.{ Request, RequestBuilder }
import org.asynchttpclient.uri.Uri

object PermanentRedirectCacheKey {
  def apply(request: Request): PermanentRedirectCacheKey =
    new PermanentRedirectCacheKey(request.getUri, new Cookies(request.getCookies))
}

case class PermanentRedirectCacheKey(uri: Uri, cookies: Cookies)

object PermanentRedirectCacheSupport {
  val HttpPermanentRedirectCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.redirects"
}

trait PermanentRedirectCacheSupport {

  import PermanentRedirectCacheSupport._

  def configuration: GatlingConfiguration

  private[this] val httpPermanentRedirectCacheHandler =
    new SessionCacheHandler[PermanentRedirectCacheKey, Uri](HttpPermanentRedirectCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def addRedirect(session: Session, from: Request, to: Uri): Session =
    httpPermanentRedirectCacheHandler.addEntry(session, PermanentRedirectCacheKey(from), to)

  private[this] def permanentRedirect(session: Session, request: Request): Option[(Uri, Int)] = {

      @tailrec def permanentRedirect1(from: PermanentRedirectCacheKey, redirectCount: Int): Option[(Uri, Int)] =

        httpPermanentRedirectCacheHandler.getEntry(session, from) match {
          case Some(toUri) => permanentRedirect1(new PermanentRedirectCacheKey(toUri, from.cookies), redirectCount + 1)

          case None => redirectCount match {
            case 0 => None
            case _ => Some((from.uri, redirectCount))
          }
        }

    permanentRedirect1(PermanentRedirectCacheKey(request), 0)
  }

  private[this] def redirectRequest(request: Request, toUri: Uri): Request = {
    val requestBuilder = new RequestBuilder(request)
    requestBuilder.setUri(toUri)
    requestBuilder.build
  }

  def applyPermanentRedirect(origTx: HttpTx): HttpTx =
    if (origTx.request.config.httpComponents.httpProtocol.requestPart.cache)
      permanentRedirect(origTx.session, origTx.request.ahcRequest) match {
        case Some((targetUri, redirectCount)) =>

          val newAhcRequest = redirectRequest(origTx.request.ahcRequest, targetUri)

          origTx.copy(
            request = origTx.request.copy(ahcRequest = newAhcRequest),
            redirectCount = origTx.redirectCount + redirectCount
          )

        case None => origTx
      }
    else
      origTx
}
