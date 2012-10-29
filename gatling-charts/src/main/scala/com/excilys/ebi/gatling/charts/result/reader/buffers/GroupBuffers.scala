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

import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.charts.util.JMap

trait GroupBuffers extends Buffers {

	class GroupStack {
		val stack = new JLinkedList[(Option[Group], Long)]

		def start(groupName: Option[String], time: Long) {
			val group = groupName match {
				case Some(groupName) => Some(Group(groupName, getCurrentGroup()))
				case None => None
			}
			stack.push((group, time))
		}

		def end(executionEnd: Long) {
			val (group, executionStart) = stack.pop()

			val duration = executionEnd - executionStart
			statsGroupBuffers.putOrUpdate(group, duration, oldDuration => duration max oldDuration)
		}

		def getCurrentGroup(): Option[Group] = stack.peek()._1
	}

	val groupStacksByUserAndScenario = new JMap[(Int, String), GroupStack]
	val statsGroupBuffers = new JMap[Option[Group], Long]

	def startGroup(user: Int, scenario: String, time: Long, group: Option[String]) {
		groupStacksByUserAndScenario.getOrElseUpdate((user, scenario), new GroupStack).start(group, time)
	}

	def endGroup(user: Int, scenario: String, time: Long) {
		groupStacksByUserAndScenario.getOrElseUpdate((user, scenario), throw new IllegalAccessException).end(time)
	}

	def getCurrentGroup(user: Int, scenario: String) = groupStacksByUserAndScenario.getOrElseUpdate((user, scenario), new GroupStack).getCurrentGroup()

	def getStatsGroupBuffer(group: Option[Group]) = statsGroupBuffers.get(group)
}
