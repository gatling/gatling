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

package io.gatling.http.check

sealed abstract class HttpCheckScope(protected val priority: Int) extends Product with Serializable

object HttpCheckScope {

  implicit val ordering: Ordering[HttpCheckScope] =
    (x: HttpCheckScope, y: HttpCheckScope) => Ordering[Int].compare(x.priority, y.priority)

  case object Url extends HttpCheckScope(priority = 1)
  case object Status extends HttpCheckScope(priority = 2)
  case object Header extends HttpCheckScope(priority = 3)
  case object Chunks extends HttpCheckScope(priority = 4)
  case object Body extends HttpCheckScope(priority = 4)
  case object Time extends HttpCheckScope(priority = 5)
}
