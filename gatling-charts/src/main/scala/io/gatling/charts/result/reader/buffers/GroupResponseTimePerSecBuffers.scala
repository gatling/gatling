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

import io.gatling.charts.result.reader.GroupRecord
import io.gatling.core.result.Group
import io.gatling.core.result.message.Status

trait GroupResponseTimePerSecBuffers {

  val durationPerSecBuffers = mutable.Map.empty[BufferKey, RangeBuffer]
  val cumulatedResponseTimePerSecBuffers = mutable.Map.empty[BufferKey, RangeBuffer]

  def getGroupCumulatedResponseTimePerSecBuffers(group: Group, status: Option[Status]): RangeBuffer =
    cumulatedResponseTimePerSecBuffers.getOrElseUpdate(BufferKey(None, Some(group), status), new RangeBuffer)

  def getGroupDurationPerSecBuffers(group: Group, status: Option[Status]): RangeBuffer =
    durationPerSecBuffers.getOrElseUpdate(BufferKey(None, Some(group), status), new RangeBuffer)

  def updateGroupResponseTimePerSecBuffers(record: GroupRecord) {
    import record._
    getGroupCumulatedResponseTimePerSecBuffers(group, Some(status)).update(startDateBucket, cumulatedResponseTime)
    getGroupDurationPerSecBuffers(group, Some(status)).update(startDateBucket, duration)
  }
}
