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

import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.SuccessWrapper

class HttpCaches(implicit val configuration: GatlingConfiguration)
    extends HttpContentCache
    with PermanentRedirectCache
    with DnsCache
    with StrictLogging {

  val FlushCache: Expression[Session] = _.removeAll(
    HttpContentCache.HttpContentCacheAttributeName,
    DnsCache.DnsCacheAttributeName,
    PermanentRedirectCache.HttpPermanentRedirectCacheAttributeName).success
}
