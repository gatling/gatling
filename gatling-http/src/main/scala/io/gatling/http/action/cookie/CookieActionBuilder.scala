/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.action.cookie

import io.gatling.commons.validation._
import io.gatling.core.session._
import io.gatling.http.cache.HttpCaches
import io.gatling.http.client.ahc.uri.Uri

object CookieActionBuilder {
  private val NoBaseUrlFailure = "Neither cookie domain nor baseUrl".failure
  val DefaultPath: String = "/"

  def defaultDomain(httpCaches: HttpCaches): Expression[String] =
    session =>
      httpCaches.baseUrl(session) match {
        case Some(baseUrl) => Uri.create(baseUrl).getHost.success
        case _             => NoBaseUrlFailure
      }
}
