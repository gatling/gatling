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

import java.nio.file.Path

import io.gatling.charts.component.ComponentLibrary
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.template.{ MenuTemplate, PageTemplate }
import io.gatling.commons.shared.unstable.model.stats.RequestStatsPath
import io.gatling.commons.shared.unstable.util.ScanHelper.deepCopyPackageContent
import io.gatling.core.config.GatlingConfiguration

private[gatling] class ReportsGenerator(implicit configuration: GatlingConfiguration) {

  def generateFor(reportsGenerationInputs: ReportsGenerationInputs): Path = {
    import reportsGenerationInputs._

    val chartsFiles = new ChartsFiles(reportFolderName, configuration)

    def hasAtLeastOneRequestReported: Boolean =
      logFileReader.statsPaths.exists(_.isInstanceOf[RequestStatsPath])

    def generateMenu(): Unit = new TemplateWriter(chartsFiles.menuFile).writeToFile(new MenuTemplate().getOutput)

    def generateStats(): Unit = new StatsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance).generate()

    def generateAssertions(): Unit = new AssertionsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance).generate()

    def copyAssets(): Unit = {
      deepCopyPackageContent(ChartsFiles.GatlingAssetsStylePackage, chartsFiles.styleDirectory)
      deepCopyPackageContent(ChartsFiles.GatlingAssetsJsPackage, chartsFiles.jsDirectory)
    }

    if (!hasAtLeastOneRequestReported)
      throw new UnsupportedOperationException("There were no requests sent during the simulation, reports won't be generated")

    val reportGenerators =
      List(
        new AllSessionsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance),
        new GlobalReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance),
        new RequestDetailsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance),
        new GroupDetailsReportGenerator(reportsGenerationInputs, chartsFiles, ComponentLibrary.Instance)
      )

    copyAssets()
    generateMenu()
    PageTemplate.setRunInfo(logFileReader.runMessage, logFileReader.runEnd)
    reportGenerators.foreach(_.generate())
    generateStats()
    generateAssertions()

    chartsFiles.globalFile
  }
}
