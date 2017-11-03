/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.RoundRobin
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.util.HttpTypeCaster

object BaseUrlSupport {

  val BaseUrlAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.baseUrl"

  val WsBaseUrlAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.wsBaseUrl"
}

trait BaseUrlSupport {

  import BaseUrlSupport._

  def setBaseUrl(httpProtocol: HttpProtocol): Session => Session = {
    httpProtocol.baseUrls match {
      case Nil            => identity
      case baseUrl :: Nil => _.set(BaseUrlAttributeName, baseUrl)
      case baseUrls =>
        val it = RoundRobin(baseUrls.toVector)
        _.set(BaseUrlAttributeName, it.next())
    }
  }

  def setWsBaseUrl(httpProtocol: HttpProtocol): Session => Session = {
    httpProtocol.wsPart.wsBaseUrls match {
      case Nil            => identity
      case baseUrl :: Nil => _.set(WsBaseUrlAttributeName, baseUrl)
      case baseUrls =>
        val it = RoundRobin(baseUrls.toVector)
        _.set(WsBaseUrlAttributeName, it.next())
    }
  }

  val baseUrl: Session => Option[String] = {
    // import optimized TypeCaster
    import HttpTypeCaster._
    _(BaseUrlAttributeName).asOption[String]
  }

  val wsBaseUrl: Session => Option[String] = {
    // import optimized TypeCaster
    import HttpTypeCaster._
    _(WsBaseUrlAttributeName).asOption[String]
  }
}
