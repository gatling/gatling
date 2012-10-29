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

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

import com.excilys.ebi.gatling.charts.component.{ GroupStatistics, RequestStatistics }
import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.charts.util.JMap

object GroupContainer {
	def root(value: (GroupStatistics, RequestStatistics)) = GroupContainer("ROOT", Some(value))

	def getGroup(root: GroupContainer, group: Option[Group]) = {
		@tailrec
		def recursivelyGetGroup(parent: GroupContainer, groups: List[String]): GroupContainer = groups match {
			case head :: tail => recursivelyGetGroup(parent.groups.getOrElseUpdate(head, GroupContainer(head)), tail)
			case _ => parent
		}

		group match {
			case Some(group) => recursivelyGetGroup(root, (group.name :: group.groups).reverse)
			case None => root
		}
	}
}

case class GroupContainer(name: String,
													value: Option[(GroupStatistics, RequestStatistics)] = None,
													groups: JMap[String, GroupContainer] = new JMap[String, GroupContainer](new JLinkedHashMap[String, GroupContainer]),
													requests: ArrayBuffer[RequestStatistics] = new ArrayBuffer[RequestStatistics]) {

	def addGroup(group: Group, value: (GroupStatistics, RequestStatistics)) {
		GroupContainer.getGroup(this, group.parent).groups.putOrUpdate(group.name, GroupContainer(group.name, Some(value)), group => group.copy(value = Some(value)))
	}

	def addRequest(parent: Option[Group], request: RequestStatistics) {
		GroupContainer.getGroup(this, parent).requests += request
	}
}
