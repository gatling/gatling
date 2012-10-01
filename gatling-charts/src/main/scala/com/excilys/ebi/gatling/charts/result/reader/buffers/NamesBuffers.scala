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
package com.excilys.ebi.gatling.charts.result.reader.buffers

import com.excilys.ebi.gatling.charts.result.reader.ActionRecord

trait NamesBuffers {

	class NameBuffer {

		import scala.collection.mutable

		val map = new mutable.HashMap[String, Long]

		def update(name: String, time: Long) {

			val minTime = map.getOrElseUpdate(name, time)
			if (time < minTime)
				map.put(name, time)
		}
	}

	val requestNameBuffer = new NameBuffer
	val scenarioNameBuffer = new NameBuffer

	def addNames(record: ActionRecord) {
		requestNameBuffer.update(record.request, record.executionStart)
		scenarioNameBuffer.update(record.scenario, record.executionStart)
	}
}