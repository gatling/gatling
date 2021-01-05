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

package io.gatling.charts.report

import io.gatling.charts.stats.CountsVsTimePlot
import io.gatling.charts.util.Colors._
import io.gatling.commons.stats.{ OK, Status }
import io.gatling.commons.util.Collections._

private[charts] object ReportGenerator {

  val PercentilesColors: List[String] = List(Red, LightRed, DarkOrange, Orange, Yellow, Lime, LightLime, Green, LightBlue, Blue)
}

private[charts] abstract class ReportGenerator {
  def generate(): Unit

  def count(records: Seq[CountsVsTimePlot], status: Status): Int = records.sumBy { counts =>
    status match {
      case OK => counts.oks
      case _  => counts.kos
    }
  }
}
