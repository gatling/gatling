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

/**
 * Created by z001ljb on 12/10/15.
 */
class StatsdMetricSeriesPattern(runMessage: RunMessage, configuration: GatlingConfiguration) {

  protected def metricRootPath: MetricSeries = {
    MetricSeries.metricSeries(configuration.data.graphite.rootPathPrefix) add runMessage.simulationId
  }

  private val usersRootKey = metricRootPath add metricSeries("users")

  def usersPath(scenario: String): MetricSeries = usersRootKey add scenario

  def responsePath(requestName: String, groupHierarchy: List[String]) = metricRootPath add metricSeries(groupHierarchy.reverse) add requestName

  def activeUsers(path: MetricSeries) = path add "active"
  def waitingUsers(path: MetricSeries) = path add "waiting"
  def doneUsers(path: MetricSeries) = path add "done"
  def okResponses(path: MetricSeries) = path add "ok"
  def koResponses(path: MetricSeries) = path add "ko"
  def allResponses(path: MetricSeries) = path add "all"
}
