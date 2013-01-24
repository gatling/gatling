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
package com.excilys.ebi.gatling.charts.report

import scala.annotation.tailrec
import scala.collection.mutable

import com.excilys.ebi.gatling.charts.component.RequestStatistics
import com.excilys.ebi.gatling.core.result.Group

object Container {
	val GROUP = "GROUP"
	val REQUEST = "REQUEST"
}

trait Container

case class RequestContainer(name: String, stats: RequestStatistics) extends Container

object GroupContainer {
	def root(requestStats: RequestStatistics) = GroupContainer("ROOT", requestStats)

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
	requestStats: RequestStatistics,
	contents: mutable.Map[String, Container] = mutable.LinkedHashMap.empty) extends Container {

	def addGroup(group: Group, requestStats: RequestStatistics) {
		GroupContainer.getGroup(this, group.parent).contents += (group.name -> GroupContainer(group.name, requestStats))
	}

	def addRequest(parent: Option[Group], request: RequestStatistics) {
		GroupContainer.getGroup(this, parent).contents += (request.name -> RequestContainer(request.name, request))
	}
}
