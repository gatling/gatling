/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

private[cache] object BaseUrlSupport {

  val BaseUrlAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.baseUrl"

  val WsBaseUrlAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.wsBaseUrl"
}

private[cache] trait BaseUrlSupport {

  import BaseUrlSupport._

  def setBaseUrl(httpProtocol: HttpProtocol): Session => Session =
    httpProtocol.baseUrls match {
      case Nil        => identity
      case url :: Nil => _.set(BaseUrlAttributeName, url)
      case urls =>
        val it = RoundRobin(urls.toVector)
        _.set(BaseUrlAttributeName, it.next())
    }

  def setWsBaseUrl(httpProtocol: HttpProtocol): Session => Session = {
    httpProtocol.wsPart.wsBaseUrls match {
      case Nil        => identity
      case url :: Nil => _.set(WsBaseUrlAttributeName, url)
      case urls =>
        val it = RoundRobin(urls.toVector)
        _.set(WsBaseUrlAttributeName, it.next())
    }
  }

  val baseUrl: Session => Option[String] =
    _(BaseUrlAttributeName).asOption[String]

  val wsBaseUrl: Session => Option[String] =
    _(WsBaseUrlAttributeName).asOption[String]
}
