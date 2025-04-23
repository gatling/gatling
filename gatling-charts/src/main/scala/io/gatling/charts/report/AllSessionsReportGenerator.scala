/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import java.nio.charset.Charset

import io.gatling.charts.component.ComponentLibrary
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats.{ IntVsTimePlot, LogFileData, Series }
import io.gatling.charts.util.Color

private[charts] final class AllSessionsReportGenerator(
    logFileData: LogFileData,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary,
    charset: Charset
) extends ReportGenerator {
  def generate(): Unit = {
    val series = new Series[IntVsTimePlot]("Active Users", logFileData.numberOfActiveSessionsPerSecond(None), List(Color.Users.All))

    val javascript = componentLibrary.getAllUsersJs(logFileData.runInfo.injectStart, series)

    new TemplateWriter(chartsFiles.allSessionsFile).writeToFile(javascript, charset)
  }
}
