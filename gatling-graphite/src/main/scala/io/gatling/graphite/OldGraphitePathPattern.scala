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

package io.gatling.graphite

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer.RunMessage

class OldGraphitePathPattern(runMessage: RunMessage, configuration: GatlingConfiguration) extends GraphitePathPattern(runMessage, configuration) {

  private def removeDecimalPart(d: Double): String = {
    val i = d.toInt
    if (d == i.toDouble) String.valueOf(i)
    else String.valueOf(d)
  }

  private val usersRootKey = GraphitePath.graphitePath("users")
  private val percentiles1Name = "percentiles" + removeDecimalPart(configuration.charting.indicators.percentile1)
  private val percentiles2Name = "percentiles" + removeDecimalPart(configuration.charting.indicators.percentile2)
  private val percentiles3Name = "percentiles" + removeDecimalPart(configuration.charting.indicators.percentile3)
  private val percentiles4Name = "percentiles" + removeDecimalPart(configuration.charting.indicators.percentile4)

  val metricRootPath = GraphitePath.graphitePath(configuration.data.graphite.rootPathPrefix) / runMessage.simulationId

  val allUsersPath = usersRootKey / "allUsers"

  def usersPath(scenario: String): GraphitePath = usersRootKey / scenario

  val allResponsesPath = GraphitePath.graphitePath("allRequests")

  def responsePath(requestName: String, groupHierarchy: List[String]) = GraphitePath.graphitePath(groupHierarchy.reverse) / requestName

  protected def activeUsers(path: GraphitePath) = path / "active"
  protected def waitingUsers(path: GraphitePath) = path / "waiting"
  protected def doneUsers(path: GraphitePath) = path / "done"
  protected def okResponses(path: GraphitePath) = path / "ok"
  protected def koResponses(path: GraphitePath) = path / "ko"
  protected def allResponses(path: GraphitePath) = path / "all"
  protected def count(path: GraphitePath) = path / "count"
  protected def min(path: GraphitePath) = path / "min"
  protected def max(path: GraphitePath) = path / "max"
  protected def mean(path: GraphitePath) = path / "mean"
  protected def stdDev(path: GraphitePath) = path / "stdDev"
  protected def percentiles1(path: GraphitePath) = path / percentiles1Name
  protected def percentiles2(path: GraphitePath) = path / percentiles2Name
  protected def percentiles3(path: GraphitePath) = path / percentiles3Name
  protected def percentiles4(path: GraphitePath) = path / percentiles4Name
}
