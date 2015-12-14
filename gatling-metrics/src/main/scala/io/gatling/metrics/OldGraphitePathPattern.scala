/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
package io.gatling.metrics

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer.RunMessage
import io.gatling.metrics.MetricSeries._

class OldGraphitePathPattern(runMessage: RunMessage, configuration: GatlingConfiguration) extends GraphitePathPattern(runMessage, configuration) {

  val metricRoot = MetricSeries.metricSeries(configuration.data.graphite.rootPathPrefix) add runMessage.simulationId
  private val usersRootKey = metricRoot add metricSeries("users")

  private def removeDecimalPart(d: Double): String = {
    val i = d.toInt
    if (d == i.toDouble) String.valueOf(i)
    else String.valueOf(d)
  }

  private val percentiles1Name = "percentiles" + removeDecimalPart(configuration.charting.indicators.percentile1)
  private val percentiles2Name = "percentiles" + removeDecimalPart(configuration.charting.indicators.percentile2)
  private val percentiles3Name = "percentiles" + removeDecimalPart(configuration.charting.indicators.percentile3)
  private val percentiles4Name = "percentiles" + removeDecimalPart(configuration.charting.indicators.percentile4)

  val allUsersPath = usersRootKey add "allUsers"

  def usersPath(scenario: String): MetricSeries = usersRootKey add scenario

  val allResponsesPath = metricRoot add metricSeries("allRequests")

  def responsePath(requestName: String, groupHierarchy: List[String]) = metricRoot add metricSeries(groupHierarchy.reverse) add requestName

  protected def activeUsers(path: MetricSeries) = path add "active"
  protected def waitingUsers(path: MetricSeries) = path add "waiting"
  protected def doneUsers(path: MetricSeries) = path add "done"
  protected def okResponses(path: MetricSeries) = path add "ok"
  protected def koResponses(path: MetricSeries) = path add "ko"
  protected def allResponses(path: MetricSeries) = path add "all"
  protected def count(path: MetricSeries) = path add "count"
  protected def min(path: MetricSeries) = path add "min"
  protected def max(path: MetricSeries) = path add "max"
  protected def mean(path: MetricSeries) = path add "mean"
  protected def stdDev(path: MetricSeries) = path add "stdDev"
  protected def percentiles1(path: MetricSeries) = path add percentiles1Name
  protected def percentiles2(path: MetricSeries) = path add percentiles2Name
  protected def percentiles3(path: MetricSeries) = path add percentiles3Name
  protected def percentiles4(path: MetricSeries) = path add percentiles4Name
}
