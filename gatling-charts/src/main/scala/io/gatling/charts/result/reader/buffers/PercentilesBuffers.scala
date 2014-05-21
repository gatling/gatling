/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.result.reader.buffers

import scala.collection.mutable
import com.tdunning.math.stats.{ AVLTreeDigest, TDigest }
import io.gatling.core.result.PercentilesVsTimePlot

class PercentilesBuffers {

  val digests = mutable.Map.empty[Int, TDigest]

  def update(bucket: Int, value: Int) {
    val digest = digests.getOrElseUpdate(bucket, new AVLTreeDigest(100.0))
    digest.add(value)
  }

  def percentiles: Seq[PercentilesVsTimePlot] =
    digests
      .map {
        case (time, histogram) =>
          PercentilesVsTimePlot(
            time,
            histogram.quantile(0).toInt,
            histogram.quantile(0.25).toInt,
            histogram.quantile(0.5).toInt,
            histogram.quantile(0.75).toInt,
            histogram.quantile(0.80).toInt,
            histogram.quantile(0.85).toInt,
            histogram.quantile(0.90).toInt,
            histogram.quantile(0.95).toInt,
            histogram.quantile(0.99).toInt,
            histogram.quantile(1.0).toInt)
      }.toSeq
      .sortBy(_.time)
}
