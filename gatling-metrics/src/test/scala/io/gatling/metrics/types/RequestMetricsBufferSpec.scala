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

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.core.config.GatlingConfiguration.fakeConfig
import io.gatling.core.result.message.{ OK, KO }
import io.gatling.core.ConfigurationConstants._

@RunWith(classOf[JUnitRunner])
class RequestMetricsBufferSpec extends Specification {

  implicit val defaultConfig = fakeConfig(Map(
    CONF_CHARTING_INDICATORS_PERCENTILE1 -> 95,
    CONF_CHARTING_INDICATORS_PERCENTILE2 -> 99,
    CONF_HTTP_AHC_REQUEST_TIMEOUT_IN_MS ->60000,
    CONF_DATA_GRAPHITE_MAX_MEASURED_VALUE -> 60000))

  def allValues(m: Metrics) = Seq(m.max, m.min, m.percentile1, m.percentile2)

  "RequestMetricsBuffer" should {
    "work when there is no measure" in {
      val buff = new RequestMetricsBuffer
      val metricsByStatus = buff.metricsByStatus

      (metricsByStatus.ok must beNone) and
        (metricsByStatus.ko must beNone) and
        (metricsByStatus.all must beNone)
    }

    "work when there is one OK mesure" in {
      val buff = new RequestMetricsBuffer
      buff.add(OK, 20)

      val metricsByStatus = buff.metricsByStatus
      val okMetrics = metricsByStatus.ok.get

      (metricsByStatus.ko must beNone) and
        (metricsByStatus.all.map(_.count) must beSome(1l)) and
        (okMetrics.count must beEqualTo(1l)) and
        ((_: Double) must beCloseTo(20.0 +/- 0.01)).forall(allValues(okMetrics))
    }

    "work when there are multiple measures" in {
      val buff = new RequestMetricsBuffer
      buff.add(KO, 10)
      for (t <- 100 to 200) buff.add(OK, t)

      val metricsByStatus = buff.metricsByStatus
      val okMetrics = metricsByStatus.ok.get
      val koMetrics = metricsByStatus.ko.get
      val allMetrics = metricsByStatus.all.get

      (koMetrics.count must beEqualTo(1)) and ((_: Double) must beCloseTo(10.0 +/- 0.01)).forall(allValues(koMetrics)) and
        (allMetrics.count must beEqualTo(102l)) and
        (okMetrics.count must beEqualTo(101l)) and
        (okMetrics.min must beCloseTo(100.0 +/- 0.01)) and (okMetrics.max must beCloseTo(200.0 +/- 0.01)) and
        (okMetrics.percentile1 must beCloseTo(195.0 +/- 1)) and (okMetrics.percentile2 must beCloseTo(199.0 +/- 1))
    }

    "work when there are a large number of measures" in {
      val buff = new RequestMetricsBuffer
      for (t <- 1 to 10000) buff.add(OK, t)

      val metricsByStatus = buff.metricsByStatus
      val okMetrics = metricsByStatus.ok.get

      (okMetrics.count must beEqualTo(10000)) and
        (okMetrics.min must beCloseTo(1.0 +/- 0.01)) and (okMetrics.max must beCloseTo(10000.0 +/- 0.01)) and
        (okMetrics.percentile1 must beCloseTo(9500.0 +/- 10)) and (okMetrics.percentile2 must beCloseTo(9900.0 +/- 10))
    }

  }
}
