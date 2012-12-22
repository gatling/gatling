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
package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.BypassSimpleActionBuilder
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.result.terminator.Terminator

import grizzled.slf4j.Logging

trait Feeds[B] extends Execs[B] with Logging {

	/**
	 * Method used to load data from a feeder in the current scenario
	 *
	 * @param feeder the feeder from which the values will be loaded
	 */
	def feed(feeder: Feeder[_]): B = {

		val byPass = BypassSimpleActionBuilder(session => {
			if (!feeder.hasNext) {
				error("Feeder is now empty, stopping engine")
				Terminator.forceTermination
			}

			session.set(feeder.next)
		})

		newInstance(byPass :: actionBuilders)
	}
}