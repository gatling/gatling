package io.gatling.charts.template

import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.core.assertion.AssertionResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.writer.RunMessage

class AssertionsJsonTemplate(runMessage: RunMessage, scenarioNames: List[String], assertionResults: List[AssertionResult])(implicit configuration: GatlingConfiguration) {

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
    fast"""{
  "simulation": "${runMessage.simulationClassName}",
  "simulationId": "${runMessage.simulationId}",
  "start": ${runMessage.start},
  "description": "${runMessage.runDescription}",
  "scenarios": [${scenarioNames.map(n => s""""$n"""").mkString(", ")}],
  "assertions": [
${assertionResults.map(print).mkFastring(",\n")}
  ]
}"""
  }
}
