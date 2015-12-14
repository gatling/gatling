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
package io.gatling.metrics.graphite

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer.RunMessage
import io.gatling.metrics.MetricSeries
import io.gatling.metrics.types.{ MetricByStatus, Metrics, UserBreakdown }

abstract class GraphitePathPattern(runMessage: RunMessage, configuration: GatlingConfiguration) {

  def allUsersPath: MetricSeries
  def usersPath(scenario: String): MetricSeries
  def allResponsesPath: MetricSeries
  def responsePath(requestName: String, groups: List[String]): MetricSeries

  def metrics(userBreakdowns: Map[MetricSeries, UserBreakdown], responseMetricsByStatus: Map[MetricSeries, MetricByStatus]): Iterator[(String, Long)] = {

    val userMetrics = userBreakdowns.iterator.flatMap(byProgress)

    val targetResponseMetrics =
      if (configuration.data.graphite.light)
        responseMetricsByStatus.get(allResponsesPath).map(m => Iterator.single(allResponsesPath -> m)).getOrElse(Iterator.empty)
      else
        responseMetricsByStatus.iterator

    val responseMetrics = targetResponseMetrics.flatMap(byStatus).flatMap(byMetric)

    (userMetrics ++ responseMetrics)
      .map { case (seriesTree, value) => seriesTree.bucket -> value }
  }

  private def byProgress(metricsEntry: (MetricSeries, UserBreakdown)): Seq[(MetricSeries, Long)] = {
    val (metricSeries, usersBreakdown) = metricsEntry
    Seq(
      activeUsers(metricSeries) -> usersBreakdown.active,
      waitingUsers(metricSeries) -> usersBreakdown.waiting,
      doneUsers(metricSeries) -> usersBreakdown.done
    )
  }

  private def byStatus(metricsEntry: (MetricSeries, MetricByStatus)): Seq[(MetricSeries, Option[Metrics])] = {
    val (metricSeries, metricByStatus) = metricsEntry
    Seq(
      okResponses(metricSeries) -> metricByStatus.ok,
      koResponses(metricSeries) -> metricByStatus.ko,
      allResponses(metricSeries) -> metricByStatus.all
    )
  }

  private def byMetric(metricsEntry: (MetricSeries, Option[Metrics])): Seq[(MetricSeries, Long)] =
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

  protected def metricRoot: MetricSeries
  protected def activeUsers(path: MetricSeries): MetricSeries
  protected def waitingUsers(path: MetricSeries): MetricSeries
  protected def doneUsers(path: MetricSeries): MetricSeries
  protected def okResponses(path: MetricSeries): MetricSeries
  protected def koResponses(path: MetricSeries): MetricSeries
  protected def allResponses(path: MetricSeries): MetricSeries
  protected def count(path: MetricSeries): MetricSeries
  protected def min(path: MetricSeries): MetricSeries
  protected def max(path: MetricSeries): MetricSeries
  protected def mean(path: MetricSeries): MetricSeries
  protected def stdDev(path: MetricSeries): MetricSeries
  protected def percentiles1(path: MetricSeries): MetricSeries
  protected def percentiles2(path: MetricSeries): MetricSeries
  protected def percentiles3(path: MetricSeries): MetricSeries
  protected def percentiles4(path: MetricSeries): MetricSeries
}
