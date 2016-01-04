/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import io.gatling.commons.stats.{ Group, Status }

private[stats] trait RequestPercentilesBuffers {
  this: Buckets =>

  val responseTimePercentilesBuffers = mutable.Map.empty[BufferKey, PercentilesBuffers]

  def getResponseTimePercentilesBuffers(requestName: Option[String], group: Option[Group], status: Status): PercentilesBuffers =
    responseTimePercentilesBuffers.getOrElseUpdate(BufferKey(requestName, group, Some(status)), new PercentilesBuffers(buckets))

  private def updateRequestPercentilesBuffers(requestName: Option[String], group: Option[Group], status: Status, requestStartBucket: Int, responseTime: Int): Unit = {
    val responseTimePercentilesBuffers = getResponseTimePercentilesBuffers(requestName, group, status)
    responseTimePercentilesBuffers.update(requestStartBucket, responseTime)
  }

  def updateRequestPercentilesBuffers(record: RequestRecord): Unit = {
    import record._
    updateRequestPercentilesBuffers(Some(name), group, status, startBucket, responseTime)
    updateRequestPercentilesBuffers(None, None, status, startBucket, responseTime)
  }
}
