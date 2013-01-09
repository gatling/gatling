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

import java.util.{ HashMap => JHashMap, LinkedList => JLinkedList }

import scala.collection.JavaConversions._
import scala.collection.mutable

import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.charts.result.reader.GroupRecord
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK, RequestStatus }

trait GroupBuffers {

	class GroupStack {
		case class GroupStackEntry(record: GroupRecord, group: Group, status: RequestStatus)

		val stack = new JLinkedList[GroupStackEntry]

		def start(record: GroupRecord) {
			stack.push(GroupStackEntry(record, Group(record.group, getCurrentGroup), OK))
		}

		def end = stack.pop()

		def getCurrentGroup(): Option[Group] = if (stack.isEmpty) None else Some(stack.peek.group)

		def failed() {
			if (!stack.isEmpty && stack.peek.status == OK)
				stack.push(stack.pop.copy(status = KO))
		}
	}

	val groupStacksByUserAndScenario: mutable.Map[(Int, String), GroupStack] = new JHashMap[(Int, String), GroupStack]
	val statsGroupBuffers: mutable.Map[Option[Group], Long] = new JHashMap[Option[Group], Long]

	def startGroup(record: GroupRecord) {
		groupStacksByUserAndScenario.getOrElseUpdate((record.user, record.scenario), new GroupStack).start(record)
	}

	def endGroup(record: GroupRecord) =
		groupStacksByUserAndScenario.getOrElseUpdate((record.user, record.scenario), throw new IllegalAccessException).end

	private def groupStack(user: Int, scenario: String) = groupStacksByUserAndScenario.getOrElseUpdate((user, scenario), new GroupStack)

	def getCurrentGroup(user: Int, scenario: String) = groupStack(user, scenario).getCurrentGroup()

	def currentGroupFailed(user: Int, scenario: String) {
		groupStack(user, scenario).failed
	}
}
