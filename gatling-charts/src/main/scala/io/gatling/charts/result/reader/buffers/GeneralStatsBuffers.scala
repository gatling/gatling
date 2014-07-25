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
import io.gatling.charts.result.reader.{ RequestRecord, FileDataReader }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.Group
import io.gatling.core.result.message.Status
import io.gatling.core.result.reader.GeneralStats
import io.gatling.charts.result.reader.GroupRecord
import com.tdunning.math.stats.AVLTreeDigest

abstract class GeneralStatsBuffers(durationInSec: Long) {

  val requestGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
  val groupDurationGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
  val groupCumulatedResponseTimeGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
  val requestCounts = mutable.Map.empty[BufferKey, (Int, Int)]

  def getRequestGeneralStatsBuffers(request: Option[String], group: Option[Group], status: Option[Status]): GeneralStatsBuffer =
    requestGeneralStatsBuffers.getOrElseUpdate(BufferKey(request, group, status), new GeneralStatsBuffer(durationInSec))

  def getGroupDurationGeneralStatsBuffers(group: Group, status: Option[Status]): GeneralStatsBuffer =
    groupDurationGeneralStatsBuffers.getOrElseUpdate(BufferKey(None, Some(group), status), new GeneralStatsBuffer(durationInSec))

  def getGroupCumulatedResponseTimeGeneralStatsBuffers(group: Group, status: Option[Status]): GeneralStatsBuffer =
    groupCumulatedResponseTimeGeneralStatsBuffers.getOrElseUpdate(BufferKey(None, Some(group), status), new GeneralStatsBuffer(durationInSec))

  def getGroupRequestCounts(group: Group): (Int, Int) =
    requestCounts.getOrElseUpdate(BufferKey(None, Some(group), None), (0, 0))

  def updateRequestGeneralStatsBuffers(record: RequestRecord): Unit = {
    import record._
    getRequestGeneralStatsBuffers(Some(name), group, None).update(responseTime)
    getRequestGeneralStatsBuffers(Some(name), group, Some(status)).update(responseTime)

    getRequestGeneralStatsBuffers(None, None, None).update(responseTime)
    getRequestGeneralStatsBuffers(None, None, Some(status)).update(responseTime)
  }

  def updateGroupGeneralStatsBuffers(record: GroupRecord): Unit = {
    import record._
    getGroupCumulatedResponseTimeGeneralStatsBuffers(group, None).update(cumulatedResponseTime)
    getGroupCumulatedResponseTimeGeneralStatsBuffers(group, Some(status)).update(cumulatedResponseTime)
    getGroupDurationGeneralStatsBuffers(group, None).update(duration)
    getGroupDurationGeneralStatsBuffers(group, Some(status)).update(duration)
    val (okCount, koCount) = getGroupRequestCounts(group)
    val counts = (okCount + oks, koCount + kos)
    requestCounts += BufferKey(None, Some(group), None) -> counts
  }
}

class GeneralStatsBuffer(duration: Long) extends CountBuffer {
  val digest = new AVLTreeDigest(100.0)
  var sumOfSquares = 0L
  var sum = 0L

  override def update(time: Int): Unit = {
    super.update(time)
    digest.add(time)
    sumOfSquares += time * time
    sum += time
  }

  lazy val stats: GeneralStats = {
    val valuesCount = digest.size.toInt
    if (valuesCount == 0) {
      GeneralStats.NoPlot

    } else {
      val count = digest.size
      val mean = (sum / count).toInt
      val stdDev = math.sqrt((sumOfSquares - (sum * sum) / count) / count).toInt
      val meanRequestsPerSec = valuesCount / (duration / FileDataReader.SecMillisecRatio)

      val percentile1 = digest.quantile(configuration.charting.indicators.percentile1 / 100.0).toInt
      val percentile2 = digest.quantile(configuration.charting.indicators.percentile2 / 100.0).toInt
      val min = digest.quantile(0).toInt
      val max = digest.quantile(1).toInt

      GeneralStats(min.toInt, max.toInt, valuesCount, mean, stdDev, percentile1, percentile2, meanRequestsPerSec)
    }
  }
}
