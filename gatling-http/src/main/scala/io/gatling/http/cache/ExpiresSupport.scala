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

import io.gatling.commons.util.NumberHelper._
import io.gatling.commons.util.TimeHelper.unpreciseNowMillis
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.response.Response

import org.asynchttpclient.cookie.DateParser

trait ExpiresSupport {

  val MaxAgePrefix = "max-age="
  val MaxAgeZero = MaxAgePrefix + "0"

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

    Option(DateParser.parse(trimmedTimeString)).map(_.getTime)
  }

  def getResponseExpires(response: Response): Option[Long] = {
      def pragmaNoCache = response.header(HeaderNames.Pragma).exists(_.contains(HeaderValues.NoCache))
      def cacheControlNoCache = response.header(HeaderNames.CacheControl)
        .exists(h => h.contains(HeaderValues.NoCache) || h.contains(HeaderValues.NoStore) || h.contains(MaxAgeZero))
      def maxAgeAsExpiresValue = response.header(HeaderNames.CacheControl).flatMap(extractMaxAgeValue).map { maxAge =>
        if (maxAge < 0)
          maxAge
        else
          maxAge * 1000 + unpreciseNowMillis
      }
      def expiresValue = response.header(HeaderNames.Expires).flatMap(extractExpiresValue).filter(_ > unpreciseNowMillis)

    if (pragmaNoCache || cacheControlNoCache) {
      None
    } else {
      // If a response includes both an Expires header and a max-age directive, the max-age directive overrides the Expires header,
      // even if the Expires header is more restrictive. (http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3)
      maxAgeAsExpiresValue.orElse(expiresValue).filter(_ > 0)
    }
  }
}
