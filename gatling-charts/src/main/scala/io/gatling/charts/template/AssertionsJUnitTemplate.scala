package io.gatling.charts.template

import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.core.assertion.AssertionResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.writer.RunMessage

class AssertionsJUnitTemplate(runMessage: RunMessage, assertionResults: List[AssertionResult])(implicit configuration: GatlingConfiguration) {

  private[this] def printMessage(assertionResult: AssertionResult): Fastring =
    if (assertionResult.result)
      fast"""<system-out>${assertionResult.message}</system-out>"""
    else
      fast"""<failure type="${assertionResult.assertion.path.printable(configuration)}">Actual values: ${assertionResult.values.mkString(", ")}</failure>"""

  private[this] def print(assertionResult: AssertionResult): Fastring =
    fast"""<testcase name="${assertionResult.message}" status="${assertionResult.result}" time="0">
  ${printMessage(assertionResult)}
</testcase>"""

  def getOutput: Fastring = {
    fast"""<testsuite name="${runMessage.simulationClassName}" tests="${assertionResults.size}" errors="0" failures="${assertionResults.filter(_.result == false).size}" time="0">
${assertionResults.map(print).mkFastring("\n")}
</testsuite>"""
  }
}
