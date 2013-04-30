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

import io.gatling.core.session.Session
import io.gatling.core.validation.Validation
import io.gatling.core.util.TimeHelper.nowMillis

trait Loop {

	def counterName: String
	val timestampName = "timestamp." + counterName

	def counterValue(session: Session) = session.get[Int](counterName, -1)
	def timestampValue(session: Session) = session.get[Long](timestampName, 0L)

	def incrementLoop(session: Session): Session = {
		val value = counterValue(session)
		if (value == -1)
			session.setAll(counterName -> 0, timestampName -> nowMillis)
		else
			session.set(counterName, value + 1)
	}

	def exitLoop(session: Session): Session = session.remove(counterName)
}