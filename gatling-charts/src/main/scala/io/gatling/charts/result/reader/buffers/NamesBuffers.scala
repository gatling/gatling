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
package io.gatling.charts.result.reader.buffers

import scala.collection.mutable

import io.gatling.charts.result.reader.{ GroupRecord, RequestRecord, UserRecord }
import io.gatling.core.result.{ GroupStatsPath, RequestStatsPath, StatsPath }

trait NamesBuffers {

  class NameBuffer[A] {

    val map = mutable.Map.empty[A, Long]

    def update(name: A, time: Long) {
      map += (name -> (time min map.getOrElse(name, Long.MaxValue)))
    }
  }

  val groupAndRequestsNameBuffer = new NameBuffer[StatsPath]
  val scenarioNameBuffer = new NameBuffer[String]

  def addScenarioName(record: UserRecord) {
    import record._
    scenarioNameBuffer.update(scenario, startDate)
  }

  def addRequestName(record: RequestRecord) {
    import record._
    groupAndRequestsNameBuffer.update(RequestStatsPath(name, group), requestStart)
  }

  def addGroupName(record: GroupRecord) {
    import record._
    groupAndRequestsNameBuffer.update(GroupStatsPath(group), startDate)
  }
}
