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
import java.time.ZoneId

import io.gatling.charts.component._
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats._
import io.gatling.charts.template.GlobalPageTemplate
import io.gatling.commons.stats.OK
import io.gatling.core.config.ReportsConfiguration
import io.gatling.shared.model.assertion.AssertionResult

private[charts] final class GlobalReportGenerator(
    logFileData: LogFileData,
    assertionResults: List[AssertionResult],
    rootContainer: GroupContainer,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary,
    zoneId: ZoneId,
    charset: Charset,
    configuration: ReportsConfiguration
) extends ReportGenerator {

  private def userStartRateComponent(logFileData: LogFileData) = {
    val userStartRateSeries = logFileData.scenarioNames.map { scenarioName =>
      new UserSeries(scenarioName, logFileData.userStartRatePerSecond(Some(scenarioName)))
    }.reverse

    componentLibrary.getUserStartRateComponent(
      logFileData.runInfo.injectStart,
      new UserSeries("All users", logFileData.userStartRatePerSecond(None)),
      userStartRateSeries
    )
  }

  private def maxNumberOfConcurrentUsersComponent(logFileData: LogFileData) = {
    val userStartRateSeries: Seq[UserSeries] = logFileData.scenarioNames.map { scenarioName =>
      new UserSeries(scenarioName, logFileData.maxNumberOfConcurrentUsersPerSecond(Some(scenarioName)))
    }.reverse

    componentLibrary.getMaxConcurrentUsersComponent(
      logFileData.runInfo.injectStart,
      new UserSeries("All users", logFileData.maxNumberOfConcurrentUsersPerSecond(None)),
      userStartRateSeries
    )
  }

  private def responseTimeDistributionChartComponent(logFileData: LogFileData): Component = {
    val (okDistribution, koDistribution) = logFileData.responseTimeDistribution(100, None, None)
    componentLibrary.getDistributionComponent("Response Time", "Requests", okDistribution, koDistribution)
  }

  private def responseTimeChartComponent(logFileData: LogFileData): Component =
    componentLibrary.getPercentilesOverTimeComponent(
      s"Response Time Percentiles over Time (${Series.OK})",
      "Response Time",
      logFileData.runInfo.injectStart,
      logFileData.responseTimePercentilesOverTime(OK, None, None)
    )

  private def requestsChartComponent(logFileData: LogFileData): Component =
    componentLibrary.getRequestsComponent(
      logFileData.runInfo.injectStart,
      logFileData.numberOfRequestsPerSecond(None, None).sortBy(_.time)
    )

  private def responsesChartComponent(logFileData: LogFileData): Component =
    componentLibrary.getResponsesComponent(
      logFileData.runInfo.injectStart,
      logFileData.numberOfResponsesPerSecond(None, None).sortBy(_.time)
    )

  def generate(): Unit = {
    val ranges = logFileData.numberOfRequestInResponseTimeRanges(None, None)

    val template = new GlobalPageTemplate(
      logFileData.runInfo,
      rootContainer,
      new SchemaContainerComponent(
        componentLibrary.getRangesComponent("Response Time Ranges", "requests", ranges, large = false),
        componentLibrary.getRequestCountPolarComponent(rootContainer),
        new SimulationCardComponent(logFileData.runInfo, zoneId)
      ),
      new AssertionsTableComponent(assertionResults),
      new GlobalStatsTableComponent(rootContainer, configuration.indicators),
      new ErrorsTableComponent(logFileData.errors(None, None)),
      userStartRateComponent(logFileData),
      maxNumberOfConcurrentUsersComponent(logFileData),
      responseTimeDistributionChartComponent(logFileData),
      responseTimeChartComponent(logFileData),
      requestsChartComponent(logFileData),
      responsesChartComponent(logFileData)
    )

    new TemplateWriter(chartsFiles.globalFile).writeToFile(template.getOutput, charset)
  }
}
