/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.collection.mutable

import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.core.validation.Validation
import io.gatling.http.check.HttpCheckOrder.HttpCheckOrder
import io.gatling.http.response.Response

/**
 * This class serves as model for the HTTP-specific checks
 *
 * @param wrapped the underlying check
 * @param order the check priority
 */
case class HttpCheck(wrapped: Check[Response], order: HttpCheckOrder) extends Check[Response] with Ordered[HttpCheck] {
	def check(response: Response, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[Session => Session] = wrapped.check(response, session)
	def compare(that: HttpCheck) = order.compare(that.order)
}
