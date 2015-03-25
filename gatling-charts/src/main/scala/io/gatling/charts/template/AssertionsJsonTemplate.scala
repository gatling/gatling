package io.gatling.charts.template

import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.core.assertion.AssertionResult

class AssertionsJsonTemplate(assertionResults: List[AssertionResult]) {

  private[this] def print(assertionResult: AssertionResult): Fastring = {
    import assertionResult._
    fast"""{
  "path": "${assertion.path}",
  "condition": "${assertion.condition}",
  "target": "${assertion.target}",
  "result": ${result},
  "message": "${message}",
  "values": [${values.mkString(",")}]
}"""
  }

  def getOutput: Fastring = {
    fast"""[
${assertionResults.map(print).mkFastring(",\n")}
]"""
  }
}
