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

package io.gatling.charts.stats

object Series {
  val OK = "OK"
  val KO = "KO"
  val All = "All"
  val Distribution = "Distribution"
}

final class Series[X](val name: String, val data: Iterable[X], val colors: List[String])
final class IntVsTimePlot(val time: Int, val value: Int)
final class CountsVsTimePlot(val time: Int, val oks: Int, val kos: Int) {
  def total: Int = oks + kos
}
final class PercentVsTimePlot(val time: Int, val value: Double) {
  def roundedUpValue: Double = (value * 100).toInt / 100.0
}
final class PieSlice(val name: String, val value: Double)
final class PercentilesVsTimePlot(val time: Int, val percentiles: Option[Percentiles])
final class Percentiles(
    val percentile0: Int,
    val percentile25: Int,
    val percentile50: Int,
    val percentile75: Int,
    val percentile80: Int,
    val percentile85: Int,
    val percentile90: Int,
    val percentile95: Int,
    val percentile99: Int,
    val percentile100: Int
)
