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

class RequestMetricsBuffer(implicit configuration: GatlingConfiguration) {

  private val bucketsWidth = configuration.data.graphite.bucketWidth
  private val percentile1 = configuration.charting.indicators.percentile1
  private val percentile2 = configuration.charting.indicators.percentile2

  private val okDigest = new MetricsBuffer(bucketsWidth)
  private val koDigest = new MetricsBuffer(bucketsWidth)
  private val allDigest = new MetricsBuffer(bucketsWidth)

  def add(status: Status, time: Long): Unit = {
    val responseTime = time.max(0L)

    allDigest.add(responseTime)
    status match {
      case OK => okDigest.add(responseTime)
      case KO => koDigest.add(responseTime)
    }
  }

  def clear(): Unit = {
    okDigest.clear
    koDigest.clear
    allDigest.clear
  }

  private def metricsOfDigest(digest: MetricsBuffer) = if (digest.size() > 0) Some(Metrics(digest, percentile1, percentile2)) else None

  def metricsByStatus = MetricByStatus(metricsOfDigest(okDigest), metricsOfDigest(koDigest), metricsOfDigest(allDigest))
}

class MetricsBuffer(bucketWidth: Int) {

  var count = 0
  var max = 0L
  var min = Long.MaxValue
  private val buckets = mutable.HashMap.empty[Long, Long].withDefaultValue(0L)

  def add(value: Long) {
    count += 1
    max = max.max(value)
    min = min.min(value)

    val bucket = value / bucketWidth
    val newCount = buckets(bucket) + 1L
    buckets += (bucket -> newCount)
  }

  def clear(): Unit = {
    count = 0
    max = 0L
    min = Long.MaxValue
    buckets.clear
  }

  def size() = count

  def quantile(quantile: Double): Long = {
    if (buckets.isEmpty)
      0L
    else {
      val limit = (count * (quantile / bucketWidth)).toLong

        @tailrec
        def getQuantileRec(buckets: List[(Long, Long)], count: Long): Long = buckets match {
          case (bucketTime, bucketCount) :: tail =>
            val newCount = count + bucketCount
            if (newCount >= limit)
              max.min((bucketTime * bucketWidth) + bucketWidth)
            else
              getQuantileRec(tail, newCount)

          case Nil => max
        }

      getQuantileRec(buckets.toList.sorted, 0L)
    }
  }
}

case class MetricByStatus(ok: Option[Metrics], ko: Option[Metrics], all: Option[Metrics])
case class Metrics(count: Long, min: Double, max: Double, percentile1: Double, percentile2: Double)

object Metrics {
  def apply(digest: MetricsBuffer, percentile1: Double, percentile2: Double): Metrics =
    Metrics(digest.size(), digest.min, digest.max, digest.quantile(percentile1), digest.quantile(percentile2))
}

