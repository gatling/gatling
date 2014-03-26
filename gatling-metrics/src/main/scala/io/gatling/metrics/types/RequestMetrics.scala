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

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.writer.RequestMessage
import com.tdunning.math.stats.ArrayDigest
import com.tdunning.math.stats.TDigest

class RequestMetrics {

  val okMetrics = new Metrics
  val koMetrics = new Metrics
  val allMetrics = new Metrics

  def update(requestMessage: RequestMessage) {
    val responseTime = requestMessage.responseTime.max(0L)

    allMetrics.update(responseTime)

    requestMessage.status match {
      case OK => okMetrics.update(responseTime)
      case KO => koMetrics.update(responseTime)
    }
  }

  def metrics = (okMetrics, koMetrics, allMetrics)

  def reset() {
    okMetrics.reset()
    koMetrics.reset()
    allMetrics.reset()
  }
}

class Metrics(compression: Int = configuration.data.graphite.quantileCompression) {

  var count = 0L
  var max = 0L
  var min = Long.MaxValue
  var digest = TDigest.createArrayDigest(compression)

  def update(value: Long) {
    count += 1
    max = max.max(value)
    min = min.min(value)
    digest.add(value)
  }

  def reset() {
    count = 0L
    max = 0L
    min = Long.MaxValue
    digest = TDigest.createArrayDigest(compression)
  }

  def getQuantile(quantile: Int): Double =
    if (count > 0)
      digest.quantile(quantile / 100.0d)
    else
      0.0d

}
