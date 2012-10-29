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

import java.util.{ LinkedList => JLinkedList }

import com.excilys.ebi.gatling.charts.result.reader.ActionRecord
import com.excilys.ebi.gatling.core.action.StartAction
import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.charts.util.JMap

trait GroupBuffers extends Buffers {

	class GroupStack {
		val stack = new JLinkedList[(Option[Group], Long)]

		def start(request: String, executionStart: Long) {
			val group = request match {
				case StartAction.START_OF_SCENARIO => None
				case _ => Some(Group(request, getCurrentGroup()))
			}
			stack.push((group, executionStart))
		}

		def end(request: String, executionEnd: Long) {
			val (group, executionStart) = stack.pop()

			val duration = executionEnd - executionStart
			statsGroupBuffers.putOrUpdate(group, duration, oldDuration => duration max oldDuration)
		}

		def getCurrentGroup(): Option[Group] = stack.peek()._1
	}

	val groupStacksByUserAndScenario = new JMap[(Int, String), GroupStack]
	val statsGroupBuffers = new JMap[Option[Group], Long]

	def startGroup(record: ActionRecord) {
		groupStacksByUserAndScenario.getOrElseUpdate((record.user, record.scenario), new GroupStack).start(record.request, record.executionStart)
	}

	def endGroup(record: ActionRecord) {
		groupStacksByUserAndScenario.getOrElseUpdate((record.user, record.scenario), throw new IllegalAccessException).end(record.request, record.executionStart)
	}

	def getCurrentGroup(user: Int, scenario: String) = groupStacksByUserAndScenario.getOrElseUpdate((user, scenario), new GroupStack).getCurrentGroup()

	def getStatsGroupBuffer(group: Option[Group]) = statsGroupBuffers.get(group)
}
