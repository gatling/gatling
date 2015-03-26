package io.gatling.charts.template

import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.core.assertion.AssertionResult
import io.gatling.core.config.GatlingConfiguration

class AssertionsJsonTemplate(assertionResults: List[AssertionResult])(implicit configuration: GatlingConfiguration) {

  private[this] def print(assertionResult: AssertionResult): Fastring = {
    import assertionResult._
    fast"""{
  "path": "${assertion.path.printable(configuration)}",
  "target": "${assertion.target.printable(configuration)}",
  "condition": "${assertion.condition.printable(configuration)}",
  "conditionValues": [${assertion.condition.values.mkString(",")}],
  "result": $result,
  "message": "$message",
  "values": [${values.mkString(",")}]
}"""
  }

  def getOutput: Fastring = {
    fast"""[
${assertionResults.map(print).mkFastring(",\n")}
]"""
  }
}
