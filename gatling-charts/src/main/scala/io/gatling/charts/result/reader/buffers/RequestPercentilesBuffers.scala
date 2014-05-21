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
import io.gatling.core.result.Group
import io.gatling.core.result.message.Status
import io.gatling.charts.result.reader.RequestRecord

trait RequestPercentilesBuffers {

  val requestPercentilesBuffers = mutable.Map.empty[BufferKey, (PercentilesBuffers, PercentilesBuffers)]

  private def percentilesBufferPair(requestName: String, group: Option[Group], status: Status): (PercentilesBuffers, PercentilesBuffers) =
    requestPercentilesBuffers.getOrElseUpdate(BufferKey(Some(requestName), group, Some(status)), (new PercentilesBuffers, new PercentilesBuffers))

  def getResponseTimePercentilesBuffers(requestName: String, group: Option[Group], status: Status): PercentilesBuffers =
    percentilesBufferPair(requestName, group, status)._1

  def getLatencyPercentilesBuffers(requestName: String, group: Option[Group], status: Status): PercentilesBuffers =
    percentilesBufferPair(requestName, group, status)._2

  def updateRequestPercentilesBuffers(record: RequestRecord): Unit = {
    import record._
    val (responseTimeHistogramBuffers, latencyHistogramBuffers) = percentilesBufferPair(name, group, status)
    responseTimeHistogramBuffers.update(requestStartBucket, responseTime)
    latencyHistogramBuffers.update(requestStartBucket, responseTime)
  }
}
