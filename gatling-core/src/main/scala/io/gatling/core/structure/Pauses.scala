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
package io.gatling.core.structure

import scala.concurrent.duration.Duration

import io.gatling.core.action.builder.PauseBuilder
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.SuccessWrapper

trait Pauses[B] extends Execs[B] {

	/**
	 * Method used to define a pause based on a duration defined in the session
	 *
	 * @param duration Expression that when resolved, provides the pause duration
	 * @return a new builder with a pause added to its actions
	 */
	def pause(duration: Expression[Duration]): B = newInstance(new PauseBuilder(duration) :: actionBuilders)
}
