/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.report

import scala.tools.nsc.io.Path

import io.gatling.charts.component.ComponentLibrary
import io.gatling.charts.config.ChartsFiles.{ globalFile, menuFile }
import io.gatling.charts.template.{ MenuTemplate, PageTemplate }
import io.gatling.core.config.GatlingFiles._
import io.gatling.core.result.RequestStatsPath
import io.gatling.core.result.reader.DataReader
import io.gatling.core.util.ScanHelper.deepCopyPackageContent

object ReportsGenerator {

  def generateFor(outputDirectoryName: String, dataReader: DataReader): Path = {

      def generateMenu(): Unit = new TemplateWriter(menuFile(outputDirectoryName)).writeToFile(new MenuTemplate().getOutput)

      def generateStats(): Unit = new StatsReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.Instance).generate()

      def copyAssets(): Unit = {
        deepCopyPackageContent(GatlingAssetsStylePackage, styleDirectory(outputDirectoryName))
        deepCopyPackageContent(GatlingAssetsJsPackage, jsDirectory(outputDirectoryName))
      }

    if (!dataReader.statsPaths.collectFirst { case r @ RequestStatsPath(_, _) => r }.isDefined) throw new UnsupportedOperationException("There were no requests sent during the simulation, reports won't be generated")

    val reportGenerators =
      List(new AllSessionsReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.Instance),
        new GlobalReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.Instance),
        new RequestDetailsReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.Instance),
        new GroupDetailsReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.Instance))

    copyAssets()
    generateMenu()
    PageTemplate.setRunInfo(dataReader.runMessage, dataReader.runEnd)
    reportGenerators.foreach(_.generate)
    generateStats()

    globalFile(outputDirectoryName)
  }
}
