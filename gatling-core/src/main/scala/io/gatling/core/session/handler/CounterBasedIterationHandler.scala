/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
import io.gatling.core.validation.{ Failure, Success }

/**
 * This trait is used for mixin-composition
 *
 * It adds counter based iteration behavior to a class
 */
trait CounterBasedIterationHandler extends Logging {

	def counterName: String

	def init(session: Session) =
		if (session.contains(counterName)) session
		else session.set(counterName, -1)

	def increment(session: Session) = session.safeGet[Int](counterName) match {
		case Success(currentValue) =>
			session.set(counterName, currentValue + 1)
		case Failure(message) =>
			logger.error(s"Could not retrieve loop counter named $counterName: $message")
			throw new IllegalAccessError("You must call 'init' before calling 'increment'")
	}

	def expire(session: Session) = session.remove(counterName)
}