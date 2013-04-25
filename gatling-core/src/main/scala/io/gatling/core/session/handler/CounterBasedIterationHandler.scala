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
package io.gatling.core.session.handler

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.session.Session
import io.gatling.core.validation.{ SuccessWrapper, Validation }

/**
 * Adds counter based iteration behavior to a class
 */
trait CounterBasedIterationHandler extends Logging {

	def counterName: String

	def init(session: Session): Validation[Session] =
		if (session.contains(counterName)) session.success
		else session.set(counterName, -1).success

	def increment(session: Session): Validation[Session] = session.getV[Int](counterName).map(currentValue => session.set(counterName, currentValue + 1))

	def expire(session: Session): Validation[Session] = session.remove(counterName).success
}