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

final case class GetCookieDsl(
    name: Expression[String],
    domain: Option[Expression[String]],
    path: Option[String],
    secure: Boolean,
    saveAs: Option[String]
) {
  def withDomain(domain: Expression[String]): GetCookieDsl = copy(domain = Some(domain))
  def withPath(path: String): GetCookieDsl = copy(path = Some(path))
  def withSecure(secure: Boolean): GetCookieDsl = copy(secure = secure)
  def saveAs(key: String): GetCookieDsl = copy(saveAs = Some(key))
}
