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

import com.ning.http.client.uri.UriComponents

import scala.annotation.tailrec
import io.gatling.core.session.Session
import io.gatling.http.ahc.HttpTx
import com.ning.http.client.{ Request, RequestBuilder }

/**
 * @author Ivan Mushketyk
 */
object PermanentRedirect {
  def addRedirect(session: Session, from: UriComponents, to: UriComponents): Session = {
    val redirectStorage = CacheHandling.getRedirectMemoizationStore(session)
    session.set(CacheHandling.HttpRedirectMemoizationStoreAttributeName, redirectStorage + (from -> to))
  }

  private def permanentRedirect(session: Session, uri: UriComponents): Option[(UriComponents, Int)] = {
      @tailrec def permanentRedirect1(from: UriComponents, redirectCount: Int): Option[(UriComponents, Int)] = {
        val redirectMap = CacheHandling.getRedirectMemoizationStore(session)
        redirectMap.get(from) match {
          case Some(toUri) =>
            permanentRedirect1(toUri, redirectCount + 1)

          case None =>
            redirectCount match {
              case 0 => None
              case _ => Some(Pair(from, redirectCount))
            }
        }
      }

    permanentRedirect1(uri, 0)
  }

  private def redirectTransaction(origTx: HttpTx, uri: UriComponents, additionalRedirects: Int): HttpTx = {
    val newAhcRequest = redirectRequest(origTx.request.ahcRequest, uri)
    val newRequest = origTx.request.copy(ahcRequest = newAhcRequest)
    val newRedirectCount = origTx.redirectCount + additionalRedirects
    origTx.copy(request = newRequest, redirectCount = newRedirectCount)
  }

  private def redirectRequest(request: Request, toUri: UriComponents): Request = {
    val requestBuilder = new RequestBuilder(request)
    requestBuilder.setURI(toUri)
    requestBuilder.build()
  }

  def getRedirect(origTx: HttpTx): HttpTx =
    permanentRedirect(origTx.session, origTx.request.ahcRequest.getURI) match {
      case Some(Pair(targetUri, redirectCount)) => redirectTransaction(origTx, targetUri, redirectCount)
      case None                                 => origTx
    }
}
