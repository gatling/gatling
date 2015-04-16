/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import com.ning.http.client.Request
import com.ning.http.client.uri.Uri
import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.cache._
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.HeaderNames
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.Response

case class RequestCacheKey(uri: Uri, method: String)

class HttpCaches(implicit val configuration: GatlingConfiguration)
    extends HttpExpiresCache
    with HttpLastModifiedCache
    with HttpEtagCache
    with PermanentRedirectCache
    with StrictLogging {

  def cache(httpProtocol: HttpProtocol, request: Request, response: Response): Session => Session =
    if (httpProtocol.requestPart.cache) {
      val key = RequestCacheKey(request.getUri, request.getMethod)

        def updateCache[T](cacheHandler: SessionCacheHandler[RequestCacheKey, T], value: Option[T]): Session => Session =
          value match {
            case Some(v) => cacheHandler.addEntry(_, key, v)
            case None    => Session.Identity
          }

      val updateExpire = updateCache(httpExpiresCacheHandler, getResponseExpires(httpProtocol, response))
      val updateEtag = updateCache(httpEtagCacheHandler, response.header(HeaderNames.ETag))
      val updateLastModified = updateCache(httpLastModifiedCacheHandler, response.header(HeaderNames.LastModified))

      updateExpire andThen updateEtag andThen updateLastModified
    } else
      Session.Identity

  val FlushCache: Expression[Session] = _.removeAll(
    HttpExpiresCache.HttpExpiresCacheAttributeName,
    HttpLastModifiedCache.HttpLastModifiedCacheAttributeName,
    HttpEtagCache.HttpEtagCacheAttributeName).success
}
