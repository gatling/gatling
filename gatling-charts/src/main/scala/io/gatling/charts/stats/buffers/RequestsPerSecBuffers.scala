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

import io.gatling.charts.stats.RequestRecord
import io.gatling.commons.shared.unstable.model.stats.Group

private[stats] trait RequestsPerSecBuffers {
  this: Buckets =>

  val requestsPerSecBuffers = mutable.Map.empty[BufferKey, CountsBuffer]

  def getRequestsPerSecBuffer(requestName: Option[String], group: Option[Group]): CountsBuffer =
    requestsPerSecBuffers.getOrElseUpdate(BufferKey(requestName, group, None), new CountsBuffer(buckets))

  def updateRequestsPerSecBuffers(record: RequestRecord): Unit = {
    getRequestsPerSecBuffer(Some(record.name), record.group).update(record.startBucket, record.status)

    getRequestsPerSecBuffer(None, None).update(record.startBucket, record.status)
  }
}
