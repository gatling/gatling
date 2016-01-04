/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.charts.stats.buffers

import io.gatling.core.stats.{ Percentiles, PercentilesVsTimePlot }

import com.tdunning.math.stats.{ AVLTreeDigest, TDigest }

private[stats] class PercentilesBuffers(buckets: Array[Int]) {

  val digests: Array[Option[TDigest]] = Array.fill(buckets.length)(None)

  def update(bucketNumber: Int, value: Int): Unit = {

    digests(bucketNumber) match {
      case Some(digest) => digest.add(value)
      case None =>
        val digest = new AVLTreeDigest(100.0)
        digest.add(value)
        digests(bucketNumber) = Some(digest)
    }
  }

  def percentiles: Iterable[PercentilesVsTimePlot] =
    digests.view.zipWithIndex
      .map {
        case (digestO, bucketNumber) =>
          val time = buckets(bucketNumber)
          val percentiles = digestO.map { digest =>
            Percentiles(
              digest.quantile(0).toInt,
              digest.quantile(0.25).toInt,
              digest.quantile(0.5).toInt,
              digest.quantile(0.75).toInt,
              digest.quantile(0.80).toInt,
              digest.quantile(0.85).toInt,
              digest.quantile(0.90).toInt,
              digest.quantile(0.95).toInt,
              digest.quantile(0.99).toInt,
              digest.quantile(1.0).toInt
            )
          }

          PercentilesVsTimePlot(time, percentiles)
      }
}
