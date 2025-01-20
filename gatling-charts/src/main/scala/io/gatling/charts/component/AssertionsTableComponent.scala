/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.component

import io.gatling.charts.util.HtmlHelper._
import io.gatling.shared.model.assertion.{ AssertionMessage, AssertionResult }

private[charts] final class AssertionsTableComponent(assertionResults: List[AssertionResult]) extends Component {
  def js: String = """
	    $$('#container_exceptions').sortable('#container_exceptions');
    """

  private def line(assertionResult: AssertionResult, index: Int): String = {
    val message = assertionResult match {
      case AssertionResult.Resolved(assertion, _, _) =>
        AssertionMessage.message(assertion)
      case AssertionResult.ResolutionError(_, error) => error
    }

    val resultStyle = if (assertionResult.success) "ok" else "ko"

    s"""
       |<tr>
       |  <td class="error-col-1 $resultStyle total">
       |    ${message.htmlEscape}
       |    <span class="value" style="display:none">$index</span>
       |  </td>
       |  <td class="error-col-2 value $resultStyle total">${if (assertionResult.success) "OK" else "KO"}</td>
       |</tr>""".stripMargin
  }

  def html: String =
    if (assertionResults.isEmpty) {
      ""
    } else {
      s"""<div class="statistics extensible-geant collapsed">
    <div class="title">
        Assertions
    </div>
    <table id="container_assertions" class="statistics-in extensible-geant">
        <thead>
            <tr>
                <th id="assert-col-1" class="header sortable"><span>Assertion</span></th>
                <th id="assert-col-2" class="header sortable"><span>Status</span></th>
            </tr>
        </thead>
		<tbody>
		    ${assertionResults.zipWithIndex.map { case (assertionResult, index) => line(assertionResult, index) }.mkString}
		</tbody>
    </table>
</div>
"""
    }

  def jsFiles: Seq[String] = Seq.empty
}
