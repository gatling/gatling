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
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.resolver.DnsJavaNameResolver

import org.asynchttpclient.resolver.NameResolver

object DnsCache {
  val DnsCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.dns"
}

trait DnsCache {

  import DnsCache._

  def configuration: GatlingConfiguration

  def cacheDnsLookup(httpProtocol: HttpProtocol, nameResolver: NameResolver): Session => Session =
    if (httpProtocol.enginePart.perUserNameResolution)
      _.set(DnsCacheAttributeName, nameResolver)
    else
      Session.Identity

  def dnsLookupCacheEntry(session: Session): NameResolver =
    session(DnsCacheAttributeName).asOption[NameResolver].getOrElse(new DnsJavaNameResolver)
}
