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

package io.gatling.charts.template

import io.gatling.charts.stats.RunInfo
import io.gatling.commons.shared.unstable.model.stats.assertion.{ AssertionMessage, AssertionResult }
import io.gatling.commons.util.StringHelper.Eol

private[charts] final class AssertionsJUnitTemplate(runInfo: RunInfo, assertionResults: List[AssertionResult]) {

  private[this] def print(assertionResult: AssertionResult): String = {
    val message = AssertionMessage.message(assertionResult.assertion)
    val content = assertionResult match {
      case AssertionResult.Resolved(_, success, actualValue) =>
        if (success) {
          s"""<system-out>$message</system-out>"""
        } else {
          s"""<failure type="$message">Actual value: $actualValue</failure>"""
        }

      case AssertionResult.ResolutionError(_, error) =>
        s"""<failure type="$error">Actual value: N/A</failure>"""
    }

    s"""<testcase name="$message" status="${assertionResult.success}" time="0">
       |  $content
       |</testcase>""".stripMargin
  }

  def getOutput: String =
    s"""<testsuite name="${runInfo.simulationClassName}" tests="${assertionResults.size}" errors="0" failures="${assertionResults
        .count(_.success == false)}" time="0">
       |  ${assertionResults.map(print).mkString(Eol)}
       |</testsuite>""".stripMargin
}
