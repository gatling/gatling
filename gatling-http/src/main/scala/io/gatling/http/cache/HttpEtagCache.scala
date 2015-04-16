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

import com.ning.http.client.uri.Uri
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.cache.SessionCacheHandler

object HttpEtagCache {
  val HttpEtagCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.etagCache"
}

trait HttpEtagCache {

  def configuration: GatlingConfiguration

  val httpEtagCacheHandler = new SessionCacheHandler[RequestCacheKey, String](HttpEtagCache.HttpEtagCacheAttributeName, configuration.http.perUserCacheMaxCapacity)

  def getEtag(session: Session, uri: Uri, method: String): Option[String] =
    httpEtagCacheHandler.getEntry(session, RequestCacheKey(uri, method))
}
