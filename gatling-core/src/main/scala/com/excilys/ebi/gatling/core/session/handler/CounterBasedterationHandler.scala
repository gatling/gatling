/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.session.handler

import com.excilys.ebi.gatling.core.session.Session

import grizzled.slf4j.Logging
import scalaz.{ Failure, Success }

/**
 * This trait is used for mixin-composition
 *
 * It adds counter based iteration behavior to a class
 */
trait CounterBasedIterationHandler extends IterationHandler with Logging {

	override def init(session: Session) =
		if (session.contains(counterName))
			super.init(session)
		else
			super.init(session).set(counterName, -1)

	override def increment(session: Session) = session.safeGetAs[Int](counterName) match {
		case Success(currentValue) => super.increment(session).set(counterName, currentValue + 1)
		case Failure(message) => error("Could not retrieve loop counter: " + message); throw new IllegalAccessError("You must call startCounter before this method is called")
	}

	override def expire(session: Session) = super.expire(session).remove(counterName)
}