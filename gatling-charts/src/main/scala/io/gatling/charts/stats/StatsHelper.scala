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
package io.gatling.charts.stats

private[stats] object StatsHelper {
  def buckets(min: Long, max: Long, step: Double): Array[Int] = {
    val halfStep = step / 2
    (0 until math.ceil((max - min) / step).toInt).map(i => (min + step * i + halfStep).round.toInt).toArray
  }

  def step(min: Long, max: Long, maxPlots: Int): Double = {
    val range = max - min
    if (range < maxPlots) 1.0
    else range / maxPlots.toDouble
  }

  def timeToBucketNumber(start: Long, step: Double, maxPlots: Int): Long => Int =
    time => math.min(((time - start) / step).round.toInt, maxPlots - 1)
}
