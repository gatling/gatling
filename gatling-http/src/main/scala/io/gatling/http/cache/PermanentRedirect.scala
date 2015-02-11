/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.cache

import com.ning.http.client.{ Request, RequestBuilder }
import com.ning.http.client.uri.Uri
import io.gatling.core.config.GatlingConfiguration._
import io.gatling.core.util.cache.SessionCacheHandler

import scala.annotation.tailrec

import io.gatling.core.session.{ SessionPrivateAttributes, Session }
import io.gatling.http.ahc.HttpTx

object PermanentRedirect {

  val HttpPermanentRedirectCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.redirects"
  private val HttpPermanentRedirectCacheHandler = new SessionCacheHandler[Uri, Uri](HttpPermanentRedirectCacheAttributeName, configuration.http.redirectPerUserCacheMaxCapacity)

  def addRedirect(session: Session, from: Uri, to: Uri): Session =
    HttpPermanentRedirectCacheHandler.addEntry(session, from, to)

  private def permanentRedirect(session: Session, uri: Uri): Option[(Uri, Int)] = {

      @tailrec def permanentRedirect1(from: Uri, redirectCount: Int): Option[(Uri, Int)] =

        HttpPermanentRedirectCacheHandler.getEntry(session, from) match {
          case Some(toUri) => permanentRedirect1(toUri, redirectCount + 1)

          case None => redirectCount match {
            case 0 => None
            case _ => Some((from, redirectCount))
          }
        }

    permanentRedirect1(uri, 0)
  }

  private def redirectRequest(request: Request, toUri: Uri): Request = {
    val requestBuilder = new RequestBuilder(request)
    requestBuilder.setUri(toUri)
    requestBuilder.build()
  }

  def applyPermanentRedirect(origTx: HttpTx): HttpTx =
    if (origTx.request.config.protocol.requestPart.cache)
      permanentRedirect(origTx.session, origTx.request.ahcRequest.getUri) match {
        case Some((targetUri, redirectCount)) =>

          val newAhcRequest = redirectRequest(origTx.request.ahcRequest, targetUri)

          origTx.copy(request = origTx.request.copy(
            ahcRequest = newAhcRequest),
            redirectCount = origTx.redirectCount + redirectCount)

        case None => origTx
      }
    else
      origTx
}
