/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check

sealed trait HttpCheckOrder

object HttpCheckOrder {

  case object Status extends HttpCheckOrder
  case object Url extends HttpCheckOrder
  case object Time extends HttpCheckOrder
  case object Checksum extends HttpCheckOrder
  case object Header extends HttpCheckOrder
  case object Body extends HttpCheckOrder

  val orders = List(Status, Url, Time, Checksum, Header, Body)

  val httpCheckOrdering = Ordering.fromLessThan[HttpCheckOrder](orders.indexOf(_) < orders.indexOf(_))
}
