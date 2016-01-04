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

private[charts] class AssertionsJsonTemplate(runMessage: RunMessage, scenarioNames: List[String], assertionResults: List[AssertionResult]) {

  private[this] def print(assertionResult: AssertionResult): Fastring = {
    import assertionResult._
    fast"""{
  "path": "${assertion.path.printable}",
  "target": "${assertion.target.printable}",
  "condition": "${assertion.condition.printable}",
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
