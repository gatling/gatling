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
package io.gatling.core.action

import io.gatling.core.session.Session

import akka.actor.ActorRef

object TryMax {

	def apply(times: Int, next: ActorRef, counterName: String): While = {
		val continueCondition = (s: Session) => s.safeGet[Int](counterName).map(counterValue => counterValue == 0 || (s.isFailed && counterValue < times))
		new While(continueCondition, counterName, next)
	}
}