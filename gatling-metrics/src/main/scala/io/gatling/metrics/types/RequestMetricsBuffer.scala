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
package io.gatling.metrics.types

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message.{ KO, OK, Status }
import org.HdrHistogram.Histogram

class RequestMetricsBuffer(implicit configuration: GatlingConfiguration) {

  // Let's take the max of the possible timeouts and add a 1s buffer
  private val maxValue = configuration.data.graphite.maxMeasuredValue.max(
    configuration.http.ahc.requestTimeOutInMs) + 1000

  private val precision = 3
  private val percentile1 = configuration.charting.indicators.percentile1
  private val percentile2 = configuration.charting.indicators.percentile2

  private val okHistogram = new Histogram(maxValue, precision)
  private val koHistogram = new Histogram(maxValue, precision)
  private val allHistogram = new Histogram(maxValue, precision)

  def add(status: Status, time: Long): Unit = {
    val responseTime = time.max(0L)

    allHistogram.recordValue(responseTime)
    status match {
      case OK => okHistogram.recordValue(responseTime)
      case KO => koHistogram.recordValue(responseTime)
    }
  }

  def clear(): Unit = {
    okHistogram.reset()
    koHistogram.reset()
    allHistogram.reset()
  }

  def metricsByStatus: MetricByStatus =
    MetricByStatus(metricsOfHistogram(okHistogram), metricsOfHistogram(koHistogram), metricsOfHistogram(allHistogram))

  private def metricsOfHistogram(histogram: Histogram): Option[Metrics] = {
    val count = histogram.getTotalCount
    if (count > 0) {
      val percentile1Value = histogram.highestEquivalentValue(histogram.getValueAtPercentile(percentile1))
      val percentile2Value = histogram.highestEquivalentValue(histogram.getValueAtPercentile(percentile2))
      Some(Metrics(count, histogram.getMinValue, histogram.getMaxValue, percentile1Value, percentile2Value))
    } else
      None
  }
}

case class MetricByStatus(ok: Option[Metrics], ko: Option[Metrics], all: Option[Metrics])
case class Metrics(count: Long, min: Double, max: Double, percentile1: Double, percentile2: Double)
