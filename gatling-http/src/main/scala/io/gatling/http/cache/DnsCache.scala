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

import java.net.InetAddress

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.cache.SessionCacheHandler
import io.gatling.http.config.HttpProtocol

object DnsCache {
  val DnsCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.dns"
}

trait DnsCache {

  import DnsCache._

  val dnsCacheHandler = new SessionCacheHandler[String, InetAddress](DnsCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def configuration: GatlingConfiguration

  def cacheDnsLookup(httpProtocol: HttpProtocol, name: String, address: Option[InetAddress]): Session => Session =
    if (httpProtocol.enginePart.shareDnsCache)
      Session.Identity
    else address match {
      case Some(a) => dnsCacheHandler.addEntry(_, name, a)
      case _       => Session.Identity
    }

  def dnsLookupCacheEntry(session: Session, name: String): Option[InetAddress] =
    dnsCacheHandler.getEntry(session, name)
}
