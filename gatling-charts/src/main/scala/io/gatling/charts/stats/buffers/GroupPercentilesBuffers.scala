/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.charts.stats.buffers

import scala.collection.mutable

import io.gatling.charts.stats.GroupRecord
import io.gatling.commons.shared.unstable.model.stats.Group
import io.gatling.commons.stats.Status

private[stats] trait GroupPercentilesBuffers {
  this: Buckets =>

  val groupPercentilesBuffers = mutable.Map.empty[BufferKey, (PercentilesBuffers, PercentilesBuffers)]

  private def percentilesBufferPair(group: Group, status: Status): (PercentilesBuffers, PercentilesBuffers) =
    groupPercentilesBuffers.getOrElseUpdate(BufferKey(None, Some(group), Some(status)), (new PercentilesBuffers(buckets), new PercentilesBuffers(buckets)))

  def getGroupCumulatedResponseTimePercentilesBuffers(group: Group, status: Status): PercentilesBuffers =
    percentilesBufferPair(group, status)._1

  def getGroupDurationPercentilesBuffers(group: Group, status: Status): PercentilesBuffers =
    percentilesBufferPair(group, status)._2

  def updateGroupPercentilesBuffers(record: GroupRecord): Unit = {
    import record._
    val (cumulatedResponseTimePercentilesBuffers, durationPercentilesBuffers) = percentilesBufferPair(group, status)
    cumulatedResponseTimePercentilesBuffers.update(startBucket, cumulatedResponseTime)
    durationPercentilesBuffers.update(startBucket, duration)
  }
}
