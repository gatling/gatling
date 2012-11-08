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

import com.excilys.ebi.gatling.charts.result.reader.{ ScenarioRecord, ActionRecord }
import com.excilys.ebi.gatling.charts.util.JMap
import com.excilys.ebi.gatling.core.result.Group

trait NamesBuffers {

	class NameBuffer[A] {

		val map = new JMap[A, Long]

		def update(name: A, time: Long) {
			map.putOrUpdate(name, time, oldTime => oldTime min time)
		}
	}

	val groupAndRequestsNameBuffer = new NameBuffer[(Option[Group], Option[String])]
	val scenarioNameBuffer = new NameBuffer[String]

	def addScenarioName(record: ScenarioRecord) {
		scenarioNameBuffer.update(record.scenario, record.executionDate)
	}

	def addRequestName(record: ActionRecord, group: Option[Group]) {
		groupAndRequestsNameBuffer.update((group, Some(record.request)), record.executionStart)
	}

	def addGroupName(group: Group, time: Long) {
		groupAndRequestsNameBuffer.update((Some(group), None), time)
	}
}