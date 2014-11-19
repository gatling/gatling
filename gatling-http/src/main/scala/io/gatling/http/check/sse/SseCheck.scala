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
package io.gatling.http.check.sse

import io.gatling.core.check.{ Check, CheckResult }
import io.gatling.core.session.Session
import io.gatling.core.validation.Validation
import io.gatling.http.check.ws.Expectation

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

case class SseCheck(wrapped: Check[String], blocking: Boolean, timeout: FiniteDuration, expectation: Expectation) extends Check[String] {
  override def check(message: String, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = wrapped.check(message, session)

}
