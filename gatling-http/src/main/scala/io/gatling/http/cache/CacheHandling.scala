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

import com.ning.http.client.Request
import com.ning.http.client.date.RFC2616DateParser
import com.ning.http.client.uri.Uri
import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.config.GatlingConfiguration._
import io.gatling.core.session.{ Expression, Session, SessionPrivateAttributes }
import io.gatling.core.util.NumberHelper.extractLongValue
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.util.cache._
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.ahc.ThreeTenBPConverter
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.Response

object CacheHandling extends StrictLogging {

  val HttpPermanentRedirectStoreAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.redirects"

  def getPermanentRedirectStore(session: Session): Option[Cache[Uri, Uri]] =
    session(HttpPermanentRedirectStoreAttributeName).asOption[Cache[Uri, Uri]]

  def getOrCreatePermanentRedirectStore(session: Session): Cache[Uri, Uri] =
    getPermanentRedirectStore(session) match {
      case Some(store) => store
      case _           => Cache[Uri, Uri](configuration.http.redirectPerUserCacheMaxCapacity)
    }

  val HttpExpireStoreAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.expireStore"

  private def getExpireStore(session: Session): Option[Cache[Uri, Long]] =
    session(HttpExpireStoreAttributeName).asOption[Cache[Uri, Long]]

  def getOrCreateExpireStore(session: Session): Cache[Uri, Long] =
    getExpireStore(session) match {
      case Some(store) => store
      case _           => Cache[Uri, Long](configuration.http.expirePerUserCacheMaxCapacity)
    }

  def getExpire(httpProtocol: HttpProtocol, session: Session, uri: Uri): Option[Long] =
    if (httpProtocol.requestPart.cache) getExpireStore(session).flatMap(_.get(uri)) else None

  def clearExpire(session: Session, uri: Uri): Session = {
    logger.info(s"Resource $uri caching expired")
    getExpireStore(session) match {
      case Some(expireStore) => session.set(HttpExpireStoreAttributeName, expireStore - uri)
      case _                 => session
    }
  }

  val HttpLastModifiedStoreAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.lastModifiedStore"

  def getLastModifiedStore(session: Session): Option[Cache[Uri, String]] =
    session(HttpLastModifiedStoreAttributeName).asOption[Cache[Uri, String]]

  def getOrCreateLastModifiedStore(session: Session): Cache[Uri, String] =
    getLastModifiedStore(session) match {
      case Some(store) => store
      case _           => Cache[Uri, String](configuration.http.lastModifiedPerUserCacheMaxCapacity)
    }

  def getLastModified(httpProtocol: HttpProtocol, session: Session, uri: Uri): Option[String] =
    if (httpProtocol.requestPart.cache) getLastModifiedStore(session).flatMap(_.get(uri)) else None

  val HttpEtagStoreAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.etagStore"

  def getEtagStore(session: Session): Option[Cache[Uri, String]] =
    session(HttpEtagStoreAttributeName).asOption[Cache[Uri, String]]

  def getOrCreateEtagStore(session: Session): Cache[Uri, String] = getEtagStore(session) match {
    case Some(store) => store
    case _           => Cache[Uri, String](configuration.http.etagPerUserCacheMaxCapacity)
  }

  def getEtag(httpProtocol: HttpProtocol, session: Session, uri: Uri): Option[String] =
    if (httpProtocol.requestPart.cache) getEtagStore(session).flatMap(_.get(uri)) else None

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

    Option(new RFC2616DateParser(trimmedTimeString).parse).map(ThreeTenBPConverter.toTime)
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

      val uri = request.getUri

      val updateExpire: Session => Session =
        getResponseExpires(httpProtocol, response) match {
          case Some(expires) =>
            session => {
              logger.debug(s"Setting Expires $expires for uri $uri")
              val expireStore = getOrCreateExpireStore(session)
              session.set(HttpExpireStoreAttributeName, expireStore + (uri -> expires))
            }

            case None => Session.Identity
        }

      val updateLastModified: Session => Session =
        response.header(HeaderNames.LastModified) match {
          case Some(lastModified) =>
            session => {
              logger.debug(s"Setting LastModified $lastModified for uri $uri")
              val lastModifiedStore = getOrCreateLastModifiedStore(session)
              session.set(HttpLastModifiedStoreAttributeName, lastModifiedStore + (uri -> lastModified))
            }

            case None => Session.Identity
        }

      val updateEtag: Session => Session =
        response.header(HeaderNames.ETag) match {
          case Some(etag) =>
            session => {
              logger.debug(s"Setting Etag $etag for uri $uri")
              val etagStore = getOrCreateEtagStore(session)
              session.set(HttpEtagStoreAttributeName, etagStore + (uri -> etag))
            }

            case None => Session.Identity
        }

      updateExpire andThen updateEtag andThen updateLastModified
    } else
      Session.Identity

  val FlushCache: Expression[Session] = _.removeAll(HttpExpireStoreAttributeName, HttpLastModifiedStoreAttributeName, HttpEtagStoreAttributeName).success
}
