/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

import java.lang.System.currentTimeMillis

import io.gatling.app.cli.StatusCode
import io.gatling.charts.report.{ ReportsGenerator, ReportsGenerationInputs }
import io.gatling.charts.stats.FileDataReader
import io.gatling.commons.stats.assertion.{ AssertionValidator, AssertionResult }
import io.gatling.core.config.GatlingConfiguration

trait ResultsProcessor {

  def processResults(runResult: RunResult): StatusCode
}

class DefaultResultsProcessor(implicit configuration: GatlingConfiguration) extends ResultsProcessor {

  override def processResults(runResult: RunResult): StatusCode = {
    val start = currentTimeMillis

    initDataReader(runResult) match {
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

  private def initDataReader(runResult: RunResult): Option[FileDataReader] =
    if (reportsGenerationEnabled || runResult.hasAssertions)
      Some(new FileDataReader(runResult.runId))
    else
      None

  private def reportsGenerationEnabled =
    configuration.data.fileDataWriterEnabled && !configuration.charting.noReports

  private def generateReports(reportsGenerationInputs: ReportsGenerationInputs, start: Long): Unit = {
    println("Generating reports...")
    val indexFile = new ReportsGenerator().generateFor(reportsGenerationInputs)
    println(s"Reports generated in ${(currentTimeMillis - start) / 1000}s.")
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
