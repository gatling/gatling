/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.stats.assertion.AssertionResult
import io.gatling.core.stats.writer.RunMessage

import com.dongxiguo.fastring.Fastring.Implicits._

private[charts] class AssertionsJUnitTemplate(runMessage: RunMessage, assertionResults: List[AssertionResult]) {

  private[this] def printMessage(assertionResult: AssertionResult): Fastring =
    if (assertionResult.result)
      fast"""<system-out>${assertionResult.message}</system-out>"""
    else
      fast"""<failure type="${assertionResult.assertion.path.printable}">Actual values: ${assertionResult.values.mkString(", ")}</failure>"""

  private[this] def print(assertionResult: AssertionResult): Fastring =
    fast"""<testcase name="${assertionResult.message}" status="${assertionResult.result}" time="0">
  ${printMessage(assertionResult)}
</testcase>"""

  def getOutput: Fastring = {
    fast"""<testsuite name="${runMessage.simulationClassName}" tests="${assertionResults.size}" errors="0" failures="${assertionResults.count(_.result == false)}" time="0">
${assertionResults.map(print).mkFastring("\n")}
</testsuite>"""
  }
}
