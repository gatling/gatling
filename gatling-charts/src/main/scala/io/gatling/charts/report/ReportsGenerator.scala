/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import java.nio.file.Path

import io.gatling.charts.component.ComponentLibrary
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats.RequestStatsPath
import io.gatling.core.config.GatlingConfiguration
import io.gatling.shared.util.ScanHelper

private[gatling] class ReportsGenerator(configuration: GatlingConfiguration) {
  def generateFor(reportsGenerationInputs: ReportsGenerationInputs): Path = {
    import reportsGenerationInputs._

    val chartsFiles = new ChartsFiles(reportFolderName, configuration)

    def hasAtLeastOneRequestReported: Boolean =
      logFileData.statsPaths.exists(_.isInstanceOf[RequestStatsPath])

    def generateStats(): Unit = new StatsReportGenerator(reportsGenerationInputs, chartsFiles, configuration).generate()

    def generateAssertions(): Unit = new AssertionsReportGenerator(reportsGenerationInputs, chartsFiles, configuration).generate()

    def copyAssets(): Unit = {
      ScanHelper.deepCopyPackageContent(ChartsFiles.GatlingAssetsStylePackage, chartsFiles.styleDirectory)
      ScanHelper.deepCopyPackageContent(ChartsFiles.GatlingAssetsJsPackage, chartsFiles.jsDirectory)
    }

    if (!hasAtLeastOneRequestReported)
      throw new UnsupportedOperationException("There were no requests sent during the simulation, reports won't be generated")

    val reportGenerators =
      List(
        new AllSessionsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance, configuration),
        new GlobalReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance, configuration),
        new RequestDetailsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance, configuration),
        new GroupDetailsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance, configuration)
      )

    copyAssets()
    reportGenerators.foreach(_.generate())
    generateStats()
    generateAssertions()

    chartsFiles.globalFile
  }
}
