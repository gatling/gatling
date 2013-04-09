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
package io.gatling.charts.result.reader.buffers

import scala.collection.mutable
import io.gatling.core.result.Group
import io.gatling.charts.result.reader.GroupRecord
import io.gatling.core.result.message.{ KO, OK, RequestStatus }

trait GroupBuffers {

	class GroupStack {
		case class GroupStackEntry(record: GroupRecord, group: Group, status: RequestStatus)

		var stack: List[GroupStackEntry] = Nil

		def start(record: GroupRecord) {
			stack = GroupStackEntry(record, Group(record.group, getCurrentGroup), OK) :: stack
		}

		def end = stack match {
			case Nil =>
				throw new UnsupportedOperationException("Calling end on empty GroupStack")
			case head :: tail =>
				stack = tail
				head
		}

		def getCurrentGroup: Option[Group] = stack.headOption.map(_.group)

		def failed {
			stack match {
				case head :: tail if head.status == OK => stack = head.copy(status = KO):: stack.tail
				case _ =>
			}
		}
	}

	val groupStacks = mutable.Map.empty[Int, GroupStack]
	val statsGroupBuffers = mutable.Map.empty[Option[Group], Long]

	def startGroup(record: GroupRecord) {
		groupStacks.getOrElseUpdate(record.user, new GroupStack).start(record)
	}

	def endGroup(record: GroupRecord) =
		groupStacks.getOrElseUpdate(record.user, throw new UnsupportedOperationException(s"Can't end group for user ${record.user} as it has not been started")).end

	private def groupStack(user: Int) = groupStacks.getOrElseUpdate(user, new GroupStack)

	def getCurrentGroup(user: Int) = groupStack(user).getCurrentGroup

	def currentGroupFailed(user: Int) {
		groupStacks.get(user).map(_.failed)
	}
}
