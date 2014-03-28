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

import scala.collection.mutable
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.writer.RequestMessage
import com.tdunning.math.stats.ArrayDigest
import com.tdunning.math.stats.TDigest
import io.gatling.core.result.message.Status

class RequestMetricsBuffer(implicit configuration: GatlingConfiguration) {

  private val compression = configuration.data.graphite.quantileCompression
  private val percentile1 = configuration.charting.indicators.percentile1 / 100.0d
  private val percentile2 = configuration.charting.indicators.percentile2 / 100.0d

  private val okDigest = TDigest.createArrayDigest(compression)
  private val koDigest = TDigest.createArrayDigest(compression)
  private val allDigest = TDigest.createArrayDigest(compression)

  def add(status: Status, time: Long): Unit = {
    val responseTime = time.max(0L)

    allDigest.add(responseTime)
    status match {
      case OK => okDigest.add(responseTime)
      case KO => koDigest.add(responseTime)
    }
  }

  private def metricsOfDigest(digest: TDigest) = if (digest.size() > 0) Some(Metrics(digest, percentile1, percentile2)) else None

  def metricsByStatus = MetricByStatus(metricsOfDigest(okDigest), metricsOfDigest(koDigest), metricsOfDigest(allDigest))
}

case class MetricByStatus(ok: Option[Metrics], ko: Option[Metrics], all: Option[Metrics])
case class Metrics(count: Int, min: Double, max: Double, percentile1: Double, percentile2: Double)

object Metrics {
  def apply(digest: TDigest, percentile1: Double, percentile2: Double): Metrics =
    Metrics(digest.size, digest.quantile(0.0d), digest.quantile(1.0d), digest.quantile(percentile1), digest.quantile(percentile2))
}

