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

import java.util.UUID

import io.gatling.core.action.builder.{ ExitHereIfFailedBuilder, TryMaxBuilder }
import io.gatling.core.structure.Loops.CounterName

trait Errors[B] extends Execs[B] {

	def exitBlockOnFail(chain: ChainBuilder): B = tryMax(1)(chain)
	def tryMax(times: Int, counter: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

		require(times >= 1, "Can't set up a max try <= 1")
		
		exec(new TryMaxBuilder(times, chain)(CounterName(counter)))
	}

	def exitHereIfFailed: B = exec(ExitHereIfFailedBuilder)
}