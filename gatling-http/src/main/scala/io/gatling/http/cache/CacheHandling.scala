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

import java.text.ParsePosition

import com.ning.http.client.Request
import com.ning.http.client.cookie.RFC2616DateParser
import com.ning.http.client.uri.Uri
import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ Expression, Session, SessionPrivateAttributes }
import io.gatling.core.util.NumberHelper.extractLongValue
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.util.cache._
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.Response

case class RequestCacheKey(uri: Uri, method: String)

object CacheHandling extends StrictLogging {

  val HttpExpireCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.expireCache"
  private val HttpExpireCacheHandler = new SessionCacheHandler[RequestCacheKey, Long](HttpExpireCacheAttributeName, configuration.http.expirePerUserCacheMaxCapacity)

  val HttpLastModifiedCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.lastModifiedCache"
  private val HttpLastModifiedCacheHandler = new SessionCacheHandler[RequestCacheKey, String](HttpLastModifiedCacheAttributeName, configuration.http.lastModifiedPerUserCacheMaxCapacity)

  val HttpEtagCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.etagCache"
  private val HttpEtagCacheHandler = new SessionCacheHandler[RequestCacheKey, String](HttpEtagCacheAttributeName, configuration.http.etagPerUserCacheMaxCapacity)

  def getExpire(session: Session, uri: Uri, method: String): Option[Long] =
    HttpExpireCacheHandler.getEntry(session, RequestCacheKey(uri, method))

  def clearExpire(session: Session, uri: Uri, method: String): Session =
    HttpExpireCacheHandler.removeEntry(session, RequestCacheKey(uri, method))

  def getLastModified(session: Session, uri: Uri, method: String): Option[String] =
    HttpLastModifiedCacheHandler.getEntry(session, RequestCacheKey(uri, method))

  def getEtag(session: Session, uri: Uri, method: String): Option[String] =
    HttpEtagCacheHandler.getEntry(session, RequestCacheKey(uri, method))

  val MaxAgePrefix = "max-age="
  val MaxAgeZero = MaxAgePrefix + "0"

  def extractExpiresValue(timestring: String): Option[Long] = {

      def removeQuote(s: String) =
        if (!s.isEmpty) {
          var start = 0
          var end = s.length

          if (s.charAt(0) == '"')
            start += 1

          if (s.charAt(s.length() - 1) == '"')
            end -= 1

          s.substring(start, end)
        } else
          s

    // FIXME use offset instead of 2 substrings
    val trimmedTimeString = removeQuote(timestring.trim)

    Option(RFC2616DateParser.get.parse(trimmedTimeString, new ParsePosition(0))).map(_.getTime)
  }

  def extractMaxAgeValue(s: String): Option[Long] = {
    val index = s.indexOf(MaxAgePrefix)
    val start = MaxAgePrefix.length + index
    if (index >= 0 && start <= s.length)
      s.charAt(start) match {
        case '-'            => Some(-1)
        case c if c.isDigit => Some(extractLongValue(s, start))
        case _              => None
      }
    else
      None
  }

  def getResponseExpires(httpProtocol: HttpProtocol, response: Response): Option[Long] = {
      def pragmaNoCache = response.header(HeaderNames.Pragma).exists(_.contains(HeaderValues.NoCache))
      def cacheControlNoCache = response.header(HeaderNames.CacheControl)
        .exists(h => h.contains(HeaderValues.NoCache) || h.contains(HeaderValues.NoStore) || h.contains(MaxAgeZero))
      def maxAgeAsExpiresValue = response.header(HeaderNames.CacheControl).flatMap(extractMaxAgeValue).map { maxAge =>
        if (maxAge < 0)
          maxAge
        else
          maxAge * 1000 + nowMillis
      }
      def expiresValue = response.header(HeaderNames.Expires).flatMap(extractExpiresValue).filter(_ > nowMillis)

    if (pragmaNoCache || cacheControlNoCache) {
      None
    } else {
      // If a response includes both an Expires header and a max-age directive, the max-age directive overrides the Expires header, 
      // even if the Expires header is more restrictive. (http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3)
      maxAgeAsExpiresValue.orElse(expiresValue).filter(_ > 0)
    }
  }

  def cache(httpProtocol: HttpProtocol, request: Request, response: Response): Session => Session =
    if (httpProtocol.requestPart.cache) {
      val key = RequestCacheKey(request.getUri, request.getMethod)

        def updateCache[T](cacheHandler: SessionCacheHandler[RequestCacheKey, T], value: Option[T]): Session => Session =
          value match {
            case Some(v) => cacheHandler.addEntry(_, key, v)
            case None    => Session.Identity
          }

      val updateExpire = updateCache(HttpExpireCacheHandler, getResponseExpires(httpProtocol, response))
      val updateEtag = updateCache(HttpEtagCacheHandler, response.header(HeaderNames.ETag))
      val updateLastModified = updateCache(HttpLastModifiedCacheHandler, response.header(HeaderNames.LastModified))

      updateExpire andThen updateEtag andThen updateLastModified
    } else
      Session.Identity

  val FlushCache: Expression[Session] = _.removeAll(HttpExpireCacheAttributeName, HttpLastModifiedCacheAttributeName, HttpEtagCacheAttributeName).success
}
