/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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
import java.nio.file.Path
import java.time.ZoneId

import io.gatling.charts.component.ComponentLibrary
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats.RequestStatsPath
import io.gatling.core.config.{ DirectoryConfiguration, ReportsConfiguration }
import io.gatling.shared.util.ScanHelper

private[gatling] final class ReportsGenerator(
    zoneId: ZoneId,
    charset: Charset,
    directoryConfiguration: DirectoryConfiguration,
    reportsConfiguration: ReportsConfiguration
) {
  def generateFor(reportsGenerationInputs: ReportsGenerationInputs): Path = {

    val chartsFiles = new ChartsFiles(reportsGenerationInputs.reportFolderName, directoryConfiguration)

    def hasAtLeastOneRequestReported: Boolean =
      reportsGenerationInputs.logFileData.statsPaths.exists(_.isInstanceOf[RequestStatsPath])

    def generateStats(): Unit = new StatsReportGenerator(reportsGenerationInputs, chartsFiles, charset, reportsConfiguration).generate()

    def generateAssertions(): Unit = new AssertionsReportGenerator(reportsGenerationInputs, chartsFiles, charset).generate()

    def copyAssets(): Unit = {
      ScanHelper.deepCopyPackageContent(ChartsFiles.GatlingAssetsStylePackage, chartsFiles.styleDirectory)
      ScanHelper.deepCopyPackageContent(ChartsFiles.GatlingAssetsJsPackage, chartsFiles.jsDirectory)
    }

    if (!hasAtLeastOneRequestReported)
      throw new UnsupportedOperationException("There were no requests sent during the simulation, reports won't be generated")

    val reportGenerators =
      List(
        new AllSessionsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance, charset),
        new GlobalReportGenerator(
          reportsGenerationInputs,
          chartsFiles,
          ComponentLibrary.Instance,
          zoneId,
          charset,
          reportsConfiguration
        ),
        new RequestDetailsReportGenerator(
          reportsGenerationInputs,
          chartsFiles,
          ComponentLibrary.Instance,
          charset,
          reportsConfiguration
        ),
        new GroupDetailsReportGenerator(
          reportsGenerationInputs,
          chartsFiles,
          ComponentLibrary.Instance,
          charset,
          reportsConfiguration
        )
      )

    copyAssets()
    reportGenerators.foreach(_.generate())
    generateStats()
    generateAssertions()

    chartsFiles.globalFile
  }
}
