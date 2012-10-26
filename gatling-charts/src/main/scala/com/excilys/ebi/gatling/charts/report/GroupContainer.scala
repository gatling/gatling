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
package com.excilys.ebi.gatling.charts.report

import java.util.{ List => JList, Map => JMap, LinkedHashMap => JLinkedHashMap, LinkedList => JLinkedList }

import scala.annotation.tailrec

import com.excilys.ebi.gatling.charts.component.{ GroupStatistics, RequestStatistics }
import com.excilys.ebi.gatling.core.result.Group

case class GroupContainer(value: Option[(GroupStatistics, RequestStatistics)] = None, groups: JMap[String, GroupContainer] = new JLinkedHashMap[String, GroupContainer], contents: JList[RequestStatistics] = new JLinkedList[RequestStatistics]) {

	def addGroup(group: Group, value: (GroupStatistics, RequestStatistics)) {
		addGroupRec(group.groups.reverse, value)
	}

	@tailrec
	private def addGroupRec(groupList: List[String], value: (GroupStatistics, RequestStatistics)) {
		if (groupList.tail.isEmpty) {
			if (groups.containsKey(groupList.head)) {
				val oldGroup = groups.get(groupList.head)
				groups.put(groupList.head, new GroupContainer(Some(value), oldGroup.groups, oldGroup.contents))
			} else groups.put(groupList.head, new GroupContainer(Some(value)))
		} else {
			if (!groups.containsKey(groupList.head)) groups.put(groupList.head, new GroupContainer)
			groups.get(groupList.head).addGroupRec(groupList.tail, value)
		}
	}

	def addContent(group: Option[Group], content: RequestStatistics) {
		group match {
			case Some(group) => addContent(group.groups.reverse, content)
			case None => contents.add(content)
		}
	}

	@tailrec
	private def addContent(groups: List[String], content: RequestStatistics) {
		if (groups.isEmpty) contents.add(content)
		else this.groups.get(groups.head).addContent(groups.tail, content)
	}
}
