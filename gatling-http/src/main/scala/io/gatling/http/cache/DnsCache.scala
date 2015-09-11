/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
import io.gatling.http.protocol.HttpProtocol

import org.asynchttpclient.channel.NameResolution

object DnsCache {
  val DnsCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.dns"
}

trait DnsCache {

  import DnsCache._

  val dnsCacheHandler = new SessionCacheHandler[String, Array[NameResolution]](DnsCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def configuration: GatlingConfiguration

  def cacheDnsLookup(httpProtocol: HttpProtocol, name: String, nameResolutions: Option[Array[NameResolution]]): Session => Session =
    if (httpProtocol.enginePart.shareDnsCache)
      Session.Identity
    else nameResolutions match {
      case Some(resolutions) => dnsCacheHandler.addEntry(_, name, resolutions)
      case _                 => Session.Identity
    }

  def dnsLookupCacheEntry(session: Session, name: String): Option[Array[NameResolution]] =
    dnsCacheHandler.getEntry(session, name).flatMap { resolutions =>
      val nonExpiredResolutions = resolutions.filter(_.expiration > System.currentTimeMillis)
      if (nonExpiredResolutions.nonEmpty)
        Some(nonExpiredResolutions)
      else
        None
    }
}
