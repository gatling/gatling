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
import io.gatling.commons.shared.unstable.model.stats.assertion.AssertionResult
import io.gatling.commons.util.StringHelper.Eol

private[charts] final class AssertionsJUnitTemplate(runInfo: RunInfo, assertionResults: List[AssertionResult]) {
  private[this] def printMessage(assertionResult: AssertionResult): String =
    if (assertionResult.result)
      s"""<system-out>${assertionResult.message}</system-out>"""
    else
      s"""<failure type="${assertionResult.assertion.path.printable}">Actual value: ${assertionResult.actualValue.getOrElse(-1.0)}</failure>"""

  private[this] def print(assertionResult: AssertionResult): String =
    s"""<testcase name="${assertionResult.message}" status="${assertionResult.result}" time="0">
  ${printMessage(assertionResult)}
</testcase>"""

  def getOutput: String =
    s"""<testsuite name="${runInfo.simulationClassName}" tests="${assertionResults.size}" errors="0" failures="${assertionResults
        .count(_.result == false)}" time="0">
${assertionResults.map(print).mkString(Eol)}
</testsuite>"""
}
