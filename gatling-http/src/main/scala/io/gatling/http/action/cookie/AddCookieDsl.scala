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

package io.gatling.http.action.cookie

import io.gatling.core.session.Expression

final case class AddCookieDsl(
    name: Expression[String],
    value: Expression[String],
    domain: Option[String],
    path: Option[String],
    maxAge: Option[Long],
    secure: Boolean
) {
  def withDomain(domain: String): AddCookieDsl = copy(domain = Some(domain))
  def withPath(path: String): AddCookieDsl = copy(path = Some(path))
  def withMaxAge(maxAge: Int): AddCookieDsl = copy(maxAge = Some(maxAge))
  def withSecure(secure: Boolean): AddCookieDsl = copy(secure = secure)
}
