/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

final class UserSeries(val name: String, val data: Seq[IntVsTimePlot])
final class IntVsTimePlot(val time: Int, val value: Int)
final class CountsVsTimePlot(val time: Int, val oks: Int, val kos: Int)
final class PercentVsTimePlot(val time: Int, val value: Double)
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
