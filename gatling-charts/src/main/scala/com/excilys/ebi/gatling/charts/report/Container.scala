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
import scala.collection.JavaConversions._
import scala.collection.mutable

import com.excilys.ebi.gatling.charts.component.{ GroupStatistics, RequestStatistics }
import com.excilys.ebi.gatling.core.result.Group

object Container {
	val GROUP = "GROUP"
	val REQUEST = "REQUEST"
}

abstract class Container

case class RequestContainer(name: String, stats: RequestStatistics) extends Container

object GroupContainer {
	def root(groupStats: GroupStatistics, requestStats: RequestStatistics) = GroupContainer("ROOT", groupStats, requestStats)

	def getGroup(root: GroupContainer, group: Option[Group]) = {
		@tailrec
		def recursivelyGetGroup(parent: GroupContainer, groups: List[String]): GroupContainer = groups match {
			case head :: tail => recursivelyGetGroup(parent.contents(head).asInstanceOf[GroupContainer], tail)
			case _ => parent
		}

		group match {
			case Some(group) => recursivelyGetGroup(root, (group.name :: group.groups).reverse)
			case None => root
		}
	}
}

case class GroupContainer(name: String,
	groupStats: GroupStatistics,
	requestStats: RequestStatistics,
	contents: mutable.Map[String, Container] = new JLinkedHashMap[String, Container]) extends Container {

	def addGroup(group: Group, groupStats: GroupStatistics, requestStats: RequestStatistics) {
		GroupContainer.getGroup(this, group.parent).contents += (group.name -> GroupContainer(group.name, groupStats, requestStats))
	}

	def addRequest(parent: Option[Group], request: RequestStatistics) {
		GroupContainer.getGroup(this, parent).contents += (request.name -> RequestContainer(request.name, request))
	}
}
