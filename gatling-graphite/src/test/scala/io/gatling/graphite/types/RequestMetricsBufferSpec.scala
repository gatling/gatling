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

import scala.collection.mutable

import io.gatling.BaseSpec
import io.gatling.commons.stats.{ KO, OK }
import io.gatling.core.ConfigKeys._
import io.gatling.core.config.GatlingConfiguration

class RequestMetricsBufferSpec extends BaseSpec {

  private val configuration = GatlingConfiguration.loadForTest(
    mutable.Map(
      charting.indicators.Percentile1 -> 95,
      charting.indicators.Percentile2 -> 99,
      http.RequestTimeout -> 60000
    )
  )

  private def allValues(m: Metrics) = Seq(m.max, m.min, m.percentile1, m.percentile2)

  "RequestMetricsBuffer" should "work when there is no measure" in {
    val buff = new HistogramRequestMetricsBuffer(configuration)
    val metricsByStatus = buff.metricsByStatus

    metricsByStatus.ok shouldBe None
    metricsByStatus.ko shouldBe None
    metricsByStatus.all shouldBe None
  }

  it should "work when there is one OK measure" in {
    val buff = new HistogramRequestMetricsBuffer(configuration)
    buff.add(OK, 20)

    val metricsByStatus = buff.metricsByStatus
    val okMetrics = metricsByStatus.ok.get

    metricsByStatus.ko shouldBe None
    metricsByStatus.all.map(_.count) shouldBe Some(1L)
    okMetrics.count shouldBe 1L
    all(allValues(okMetrics)) shouldBe 20
  }

  it should "work when there are multiple measures" in {
    val buff = new HistogramRequestMetricsBuffer(configuration)
    buff.add(KO, 10)
    for (t <- 100 to 200) buff.add(OK, t)

    val metricsByStatus = buff.metricsByStatus
    val okMetrics = metricsByStatus.ok.get
    val koMetrics = metricsByStatus.ko.get
    val allMetrics = metricsByStatus.all.get

    koMetrics.count shouldBe 1
    all(allValues(koMetrics)) shouldBe 10
    allMetrics.count shouldBe 102L
    okMetrics.count shouldBe 101L
    okMetrics.min shouldBe 100
    okMetrics.max shouldBe 200
    okMetrics.percentile1 shouldBe 195
    okMetrics.percentile2 shouldBe 199
  }

  it should "work when there are a large number of measures" in {
    val buff = new HistogramRequestMetricsBuffer(configuration)
    for (t <- 1 to 10000) buff.add(OK, t)

    val metricsByStatus = buff.metricsByStatus
    val okMetrics = metricsByStatus.ok.get

    okMetrics.count shouldBe 10000
    okMetrics.min shouldBe 1
    okMetrics.max shouldBe 10000 +- (10000 / 100) // 2 digits resolution
    okMetrics.percentile1 shouldBe 9500 +- (9500 / 100)
    okMetrics.percentile2 shouldBe 9900 +- (9900 / 100)
  }
}
