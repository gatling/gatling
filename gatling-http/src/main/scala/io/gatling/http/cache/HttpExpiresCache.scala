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

import java.text.ParsePosition

import com.ning.http.client.cookie.RFC2616DateParser
import com.ning.http.client.uri.Uri
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.NumberHelper._
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.util.cache.SessionCacheHandler
import io.gatling.http.{ HeaderValues, HeaderNames }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.Response

object HttpExpiresCache {
  val HttpExpiresCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.expiresCache"

  val MaxAgePrefix = "max-age="
  val MaxAgeZero = MaxAgePrefix + "0"
}

trait HttpExpiresCache {

  import HttpExpiresCache._

  def configuration: GatlingConfiguration

  val httpExpiresCacheHandler = new SessionCacheHandler[RequestCacheKey, Long](HttpExpiresCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def getExpires(session: Session, uri: Uri, method: String): Option[Long] =
    httpExpiresCacheHandler.getEntry(session, RequestCacheKey(uri, method))

  def clearExpires(session: Session, uri: Uri, method: String): Session =
    httpExpiresCacheHandler.removeEntry(session, RequestCacheKey(uri, method))

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
}
