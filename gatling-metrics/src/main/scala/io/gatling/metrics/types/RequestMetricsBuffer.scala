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

import scala.annotation.tailrec
import scala.collection.mutable
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message.{ KO, OK, Status }
import org.HdrHistogram.Histogram
import org.HdrHistogram.HistogramData

class RequestMetricsBuffer(implicit configuration: GatlingConfiguration) {

  // Let's take the max of the possible timeouts and add a 10% buffer 
  private val maxValue =
    (configuration.data.graphite.maxMeasuredValue
      .max(configuration.http.ahc.requestTimeOutInMs) * 1.1d).toInt

  private val precision = 3
  private val percentile1 = configuration.charting.indicators.percentile1
  private val percentile2 = configuration.charting.indicators.percentile2

  private val okDigest = new Histogram(maxValue, precision)
  private val koDigest = new Histogram(maxValue, precision)
  private val allDigest = new Histogram(maxValue, precision)

  def add(status: Status, time: Long): Unit = {
    val responseTime = time.max(0L)

    allDigest.recordValue(responseTime)
    status match {
      case OK => okDigest.recordValue(responseTime)
      case KO => koDigest.recordValue(responseTime)
    }
  }

  def clear(): Unit = {
    okDigest.reset
    koDigest.reset
    allDigest.reset
  }

  def metricsByStatus(): MetricByStatus =
    MetricByStatus(metricsOfDigest(okDigest.getHistogramData), metricsOfDigest(koDigest.getHistogramData), metricsOfDigest(allDigest.getHistogramData))

  private def metricsOfDigest(digest: HistogramData): Option[Metrics] = {
    val count = digest.getTotalCount
    if (count > 0)
      Some(Metrics(count, digest.getMinValue, digest.getMaxValue, digest.getValueAtPercentile(percentile1), digest.getValueAtPercentile(percentile2)))
    else
      None
  }
}

case class MetricByStatus(ok: Option[Metrics], ko: Option[Metrics], all: Option[Metrics])
case class Metrics(count: Long, min: Double, max: Double, percentile1: Double, percentile2: Double)
