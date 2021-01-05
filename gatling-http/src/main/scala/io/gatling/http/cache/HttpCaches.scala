/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.Clock
import io.gatling.commons.validation.SuccessWrapper
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Expression, Session }

private[http] object HttpCaches {
  val FlushCache: Expression[Session] = _.removeAll(
    HttpContentCacheSupport.HttpContentCacheAttributeName,
    PermanentRedirectCacheSupport.HttpPermanentRedirectCacheAttributeName,
    Http2PriorKnowledgeSupport.Http2PriorKnowledgeAttributeName
  ).success
}

private[http] class HttpCaches(val coreComponents: CoreComponents)
    extends HttpContentCacheSupport
    with PermanentRedirectCacheSupport
    with DnsCacheSupport
    with ResourceCacheSupport {

  override def clock: Clock = coreComponents.clock

  override def configuration: GatlingConfiguration = coreComponents.configuration
}
