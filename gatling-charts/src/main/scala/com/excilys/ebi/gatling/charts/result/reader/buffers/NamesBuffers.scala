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
import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.core.result.RequestPath
import com.excilys.ebi.gatling.charts.util.JMap

trait NamesBuffers {

	class NameBuffer[A] {

		val map = new JMap[A, Long]

		def update(name: A, time: Long) {

			val minTime = map.getOrElseUpdate(name, time)
			if (time < minTime)
				map.put(name, time)
		}
	}

	val requestPathBuffer = new NameBuffer[RequestPath]
	val groupNameBuffer = new NameBuffer[Group]
	val scenarioNameBuffer = new NameBuffer[String]

	def addNames(record: ActionRecord, group: Option[Group]) {
		requestPathBuffer.update(RequestPath(record.request, group), record.executionStart)
		scenarioNameBuffer.update(record.scenario, record.executionStart)
	}

	def addGroupName(record: ActionRecord, group: Group) {
		groupNameBuffer.update(group, record.executionStart)
	}
}