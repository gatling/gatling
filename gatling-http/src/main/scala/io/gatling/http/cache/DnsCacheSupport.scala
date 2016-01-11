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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.resolver.{ DelegatingNameResolver }
import io.gatling.http.util.HttpTypeHelper

object DnsCacheSupport {
  val DnsCacheAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.dns"
}

trait DnsCacheSupport {

  import DnsCacheSupport._

  // import optimized TypeCaster
  import HttpTypeHelper._

  def configuration: GatlingConfiguration

  def setNameResolver(httpEngine: HttpEngine): Session => Session =
    _.set(DnsCacheAttributeName, httpEngine.newDnsResolver)

  val nameResolver: (Session => Option[DelegatingNameResolver]) =
    _(DnsCacheAttributeName).asOption[DelegatingNameResolver]
}
