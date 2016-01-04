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
package io.gatling.metrics

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer.RunMessage
import io.gatling.metrics.types.{ Metrics, MetricByStatus, UserBreakdown }

abstract class GraphitePathPattern(runMessage: RunMessage, configuration: GatlingConfiguration) {

  def allUsersPath: GraphitePath
  def usersPath(scenario: String): GraphitePath
  def allResponsesPath: GraphitePath
  def responsePath(requestName: String, groups: List[String]): GraphitePath

  def metrics(userBreakdowns: Map[GraphitePath, UserBreakdown], responseMetricsByStatus: Map[GraphitePath, MetricByStatus]): Iterator[(String, Long)] = {

    val userMetrics = userBreakdowns.iterator.flatMap(byProgress)

    val targetResponseMetrics =
      if (configuration.data.graphite.light)
        responseMetricsByStatus.get(allResponsesPath).map(m => Iterator.single(allResponsesPath -> m)).getOrElse(Iterator.empty)
      else
        responseMetricsByStatus.iterator

    val responseMetrics = targetResponseMetrics.flatMap(byStatus).flatMap(byMetric)

    (userMetrics ++ responseMetrics)
      .map { case (path, value) => (metricRootPath / path).pathKey -> value }
  }

  private def byProgress(metricsEntry: (GraphitePath, UserBreakdown)): Seq[(GraphitePath, Long)] = {
    val (path, usersBreakdown) = metricsEntry
    Seq(
      activeUsers(path) -> usersBreakdown.active,
      waitingUsers(path) -> usersBreakdown.waiting,
      doneUsers(path) -> usersBreakdown.done
    )
  }

  private def byStatus(metricsEntry: (GraphitePath, MetricByStatus)): Seq[(GraphitePath, Option[Metrics])] = {
    val (path, metricByStatus) = metricsEntry
    Seq(
      okResponses(path) -> metricByStatus.ok,
      koResponses(path) -> metricByStatus.ko,
      allResponses(path) -> metricByStatus.all
    )
  }

  private def byMetric(metricsEntry: (GraphitePath, Option[Metrics])): Seq[(GraphitePath, Long)] =
    metricsEntry match {
      case (path, None) => Seq(count(path) -> 0)
      case (path, Some(m)) =>
        Seq(
          count(path) -> m.count,
          min(path) -> m.min,
          max(path) -> m.max,
          mean(path) -> m.mean,
          stdDev(path) -> m.stdDev,
          percentiles1(path) -> m.percentile1,
          percentiles2(path) -> m.percentile2,
          percentiles3(path) -> m.percentile3,
          percentiles4(path) -> m.percentile4
        )
    }

  protected def metricRootPath: GraphitePath
  protected def activeUsers(path: GraphitePath): GraphitePath
  protected def waitingUsers(path: GraphitePath): GraphitePath
  protected def doneUsers(path: GraphitePath): GraphitePath
  protected def okResponses(path: GraphitePath): GraphitePath
  protected def koResponses(path: GraphitePath): GraphitePath
  protected def allResponses(path: GraphitePath): GraphitePath
  protected def count(path: GraphitePath): GraphitePath
  protected def min(path: GraphitePath): GraphitePath
  protected def max(path: GraphitePath): GraphitePath
  protected def mean(path: GraphitePath): GraphitePath
  protected def stdDev(path: GraphitePath): GraphitePath
  protected def percentiles1(path: GraphitePath): GraphitePath
  protected def percentiles2(path: GraphitePath): GraphitePath
  protected def percentiles3(path: GraphitePath): GraphitePath
  protected def percentiles4(path: GraphitePath): GraphitePath
}
