/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.app

import io.gatling.app.cli.StatusCode
import io.gatling.charts.report.{ ReportsGenerationInputs, ReportsGenerator }
import io.gatling.charts.stats.LogFileReader
import io.gatling.commons.stats.assertion.{ AssertionResult, AssertionValidator }
import io.gatling.core.config.GatlingConfiguration

private[app] class RunResultProcessor(configuration: GatlingConfiguration) {

  private implicit val config: GatlingConfiguration = configuration

  def processRunResult(runResult: RunResult): StatusCode = {
    val start = System.currentTimeMillis()

    initLogFileReader(runResult) match {
      case Some(reader) =>
        val assertionResults = AssertionValidator.validateAssertions(reader)

        if (reportsGenerationEnabled) {
          val reportsGenerationInputs = ReportsGenerationInputs(runResult.runId, reader, assertionResults)
          generateReports(reportsGenerationInputs, start)
        }

        runStatus(assertionResults)

      case _ =>
        StatusCode.Success
    }
  }

  private def initLogFileReader(runResult: RunResult): Option[LogFileReader] =
    if (reportsGenerationEnabled || runResult.hasAssertions)
      Some(new LogFileReader(runResult.runId))
    else
      None

  private def reportsGenerationEnabled =
    configuration.core.directory.reportsOnly.isDefined || (configuration.data.fileDataWriterEnabled && !configuration.charting.noReports)

  private def generateReports(reportsGenerationInputs: ReportsGenerationInputs, start: Long): Unit = {
    println("Generating reports...")
    val indexFile = new ReportsGenerator().generateFor(reportsGenerationInputs)
    println(s"Reports generated in ${(System.currentTimeMillis() - start) / 1000}s.")
    println(s"Please open the following file: ${indexFile.toFile}")
  }

  private def runStatus(assertionResults: List[AssertionResult]): StatusCode = {
    val consolidatedAssertionResult = assertionResults.foldLeft(true) { (isValid, assertionResult) =>
      println(s"${assertionResult.message} : ${assertionResult.result}")
      isValid && assertionResult.result
    }

    if (consolidatedAssertionResult) StatusCode.Success
    else StatusCode.AssertionsFailed
  }
}
