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

package io.gatling.app

import io.gatling.app.cli.StatusCode
import io.gatling.charts.report.{ ReportsGenerationInputs, ReportsGenerator }
import io.gatling.charts.stats.{ LogFileData, LogFileReader }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.shared.model.assertion.{ AssertionMessage, AssertionResult, AssertionValidator }

private final class RunResultProcessor(configuration: GatlingConfiguration) {
  def processRunResult(runResult: RunResult): StatusCode =
    // [e]
    //
    //
    //
    // [e]
    initLogFileData(runResult) match {
      case Some(logFileData) =>
        val assertionResults = new AssertionValidator(logFileData).validateAssertions(logFileData.assertions)
        generateReports(runResult.runId, logFileData, assertionResults)
        runStatus(assertionResults)

      case _ =>
        StatusCode.Success
    }

  private def initLogFileData(runResult: RunResult): Option[LogFileData] =
    if (reportsGenerationEnabled || runResult.hasAssertions) {
      val start = System.currentTimeMillis()
      println("Parsing log file(s)...")
      val logFileData = LogFileReader(runResult.runId, configuration).read()
      println(s"Parsing log file(s) done in ${(System.currentTimeMillis() - start) / 1000}s.")
      Some(logFileData)
    } else {
      None
    }

  private def reportsGenerationEnabled: Boolean =
    configuration.core.directory.reportsOnly.isDefined || (configuration.data.fileDataWriterEnabled && !configuration.reports.noReports)

  private def generateReports(runId: String, logFileData: LogFileData, assertionResults: List[AssertionResult]): Unit =
    if (reportsGenerationEnabled) {
      println("Generating reports...")
      val reportsGenerationInputs = new ReportsGenerationInputs(runId, logFileData, assertionResults)
      val indexFile = new ReportsGenerator(configuration.data.zoneId, configuration.core.charset, configuration.core.directory, configuration.reports)
        .generateFor(reportsGenerationInputs)
      println(s"Reports generated, please open the following file: ${indexFile.toUri}")
    }

  private def runStatus(assertionResults: List[AssertionResult]): StatusCode = {
    val consolidatedAssertionResult = assertionResults.foldLeft(true) { (isValid, assertionResult) =>
      val message = AssertionMessage.message(assertionResult.assertion)
      assertionResult match {
        case AssertionResult.Resolved(_, success, actualValue) =>
          if (success) {
            println(s"$message : true (actual : $actualValue)")
          } else {
            println(s"$message : false (actual : $actualValue)")
          }
        case AssertionResult.ResolutionError(_, error) =>
          println(s"$message : false ($error)")
      }
      isValid && assertionResult.success
    }

    if (consolidatedAssertionResult) StatusCode.Success else StatusCode.AssertionsFailed
  }
}
