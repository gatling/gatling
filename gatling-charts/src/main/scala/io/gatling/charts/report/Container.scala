/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.charts.report

import scala.annotation.tailrec
import scala.collection.mutable

import io.gatling.charts.component.RequestStatistics
import io.gatling.core.result.Group

object Container {
  val GROUP = "GROUP"
  val REQUEST = "REQUEST"
}

trait Container

case class RequestContainer(name: String, stats: RequestStatistics) extends Container

object GroupContainer {
  def root(requestStats: RequestStatistics) = GroupContainer("ROOT", requestStats)
}

case class GroupContainer(name: String,
                          stats: RequestStatistics,
                          contents: mutable.Map[String, Container] = mutable.LinkedHashMap.empty) extends Container {

  private def findGroup(path: List[String]) = {

      @tailrec
      def getGroupRec(g: GroupContainer, path: List[String]): GroupContainer = path match {
        case head :: tail => getGroupRec(g.contents(head).asInstanceOf[GroupContainer], tail)
        case _            => g
      }

    getGroupRec(this, path)
  }

  def addGroup(group: Group, stats: RequestStatistics) {
    val parentGroup = group.hierarchy.dropRight(1)
    findGroup(parentGroup).contents += (group.name -> GroupContainer(group.name, stats))
  }

  def addRequest(group: Option[Group], requestName: String, stats: RequestStatistics) {
    val parentGroup = group.map(_.hierarchy).getOrElse(Nil)
    findGroup(parentGroup).contents += (requestName -> RequestContainer(requestName, stats))
  }
}
