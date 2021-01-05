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

package io.gatling.app

import io.gatling.app.cli.StatusCode
import io.gatling.charts.report.{ ReportsGenerationInputs, ReportsGenerator }
import io.gatling.charts.stats.LogFileReader
import io.gatling.commons.shared.unstable.model.stats.assertion.{ AssertionResult, AssertionValidator }
import io.gatling.core.config.GatlingConfiguration

private final class RunResultProcessor(configuration: GatlingConfiguration) {

  // [fl]
  //
  //
  // [fl]

  // [fl]
  def processRunResult(runResult: RunResult): StatusCode =
    initLogFileReader(runResult) match {
      case Some(reader) =>
        val assertionResults = AssertionValidator.validateAssertions(reader)

        if (reportsGenerationEnabled) {
          val reportsGenerationInputs = new ReportsGenerationInputs(runResult.runId, reader, assertionResults)
          generateReports(reportsGenerationInputs)
        }

        runStatus(assertionResults)

      case _ =>
        StatusCode.Success
    }

  private def initLogFileReader(runResult: RunResult): Option[LogFileReader] =
    if (reportsGenerationEnabled || runResult.hasAssertions) {
      println("Parsing log file(s)...")
      val logFileReader = new LogFileReader(runResult.runId)(configuration)
      println("Parsing log file(s) done")
      Some(logFileReader)
    } else {
      None
    }

  private def reportsGenerationEnabled: Boolean =
    configuration.core.directory.reportsOnly.isDefined || (configuration.data.fileDataWriterEnabled && !configuration.charting.noReports)

  private def generateReports(reportsGenerationInputs: ReportsGenerationInputs): Unit = {
    println("Generating reports...")
    val start = System.currentTimeMillis()
    val indexFile = new ReportsGenerator()(configuration).generateFor(reportsGenerationInputs)
    println(s"Reports generated in ${(System.currentTimeMillis() - start) / 1000}s.")
    println(s"Please open the following file: ${indexFile.toFile}")
  }

  private def runStatus(assertionResults: List[AssertionResult]): StatusCode = {
    val consolidatedAssertionResult = assertionResults.foldLeft(true) { (isValid, assertionResult) =>
      println(s"${assertionResult.message} : ${assertionResult.result}")
      isValid && assertionResult.result
    }

    if (consolidatedAssertionResult) StatusCode.Success else StatusCode.AssertionsFailed
  }
}
