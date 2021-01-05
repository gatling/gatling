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

package io.gatling.graphite.types

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.core.config.GatlingConfiguration

import org.HdrHistogram.{ AbstractHistogram, IntCountsHistogram }

class HistogramRequestMetricsBuffer(configuration: GatlingConfiguration) extends RequestMetricsBuffer {

  private val percentile1 = configuration.charting.indicators.percentile1
  private val percentile2 = configuration.charting.indicators.percentile2
  private val percentile3 = configuration.charting.indicators.percentile3
  private val percentile4 = configuration.charting.indicators.percentile4

  private val okHistogram: AbstractHistogram = new IntCountsHistogram(2)
  private val koHistogram: AbstractHistogram = new IntCountsHistogram(2)
  private val allHistogram: AbstractHistogram = new IntCountsHistogram(2)

  override def add(status: Status, time: Long): Unit = {
    val recordableTime = time.max(1L)

    allHistogram.recordValue(recordableTime)
    status match {
      case OK => okHistogram.recordValue(recordableTime)
      case KO => koHistogram.recordValue(recordableTime)
    }
  }

  override def clear(): Unit = {
    okHistogram.reset()
    koHistogram.reset()
    allHistogram.reset()
  }

  override def metricsByStatus: MetricByStatus =
    MetricByStatus(
      ok = metricsOfHistogram(okHistogram),
      ko = metricsOfHistogram(koHistogram),
      all = metricsOfHistogram(allHistogram)
    )

  private def metricsOfHistogram(histogram: AbstractHistogram): Option[Metrics] = {
    val count = histogram.getTotalCount
    if (count > 0) {
      Some(
        Metrics(
          count = count,
          min = histogram.getMinValue.toInt,
          max = histogram.getMaxValue.toInt,
          mean = histogram.getMean.toInt,
          stdDev = histogram.getStdDeviation.toInt,
          percentile1 = histogram.getValueAtPercentile(percentile1).toInt,
          percentile2 = histogram.getValueAtPercentile(percentile2).toInt,
          percentile3 = histogram.getValueAtPercentile(percentile3).toInt,
          percentile4 = histogram.getValueAtPercentile(percentile4).toInt
        )
      )
    } else
      None
  }
}
