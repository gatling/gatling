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

import java.util.{ LinkedHashMap => JLinkedHashMap }
import java.util.{ LinkedList => JLinkedList }
import java.util.{ List => JList }
import java.util.{ Map => JMap }

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.result.Group

object GroupContainer {
	type SimpleGroupContainer[A] = GroupContainer[A, A]
	type ExtendedTupleGroupContainer[A, B] = GroupContainer[(A, B), B]

	def apply[G, C](value: G) = new GroupContainer[G, C](Some(value))
}

class GroupContainer[G, C](val value: Option[G] = None, val groups: JMap[String, GroupContainer[G, C]] = new JLinkedHashMap[String, GroupContainer[G, C]], val contents: JList[C] = new JLinkedList[C]) {

	def addGroup(group: Group, value: G) {
		addGroup(group.groups.reverse, value)
	}

	@tailrec
	private def addGroup(groupList: List[String], value: G) {
		if (groupList.tail.isEmpty) {
			if (groups.containsKey(groupList.head)) {
				val oldGroup = groups.get(groupList.head)
				groups.put(groupList.head, new GroupContainer[G, C](Some(value), oldGroup.groups, oldGroup.contents))
			} else groups.put(groupList.head, new GroupContainer[G, C](Some(value)))
		} else {
			if (!groups.containsKey(groupList.head)) groups.put(groupList.head, new GroupContainer[G, C])
			groups.get(groupList.head).addGroup(groupList.tail, value)
		}
	}

	def addContent(group: Option[Group], content: C) {
		group match {
			case Some(group) => addContent(group.groups.reverse, content)
			case None => contents.add(content)
		}
	}

	@tailrec
	private def addContent(groups: List[String], content: C) {
		if (groups.isEmpty) contents.add(content)
		else this.groups.get(groups.head).addContent(groups.tail, content)
	}
}
