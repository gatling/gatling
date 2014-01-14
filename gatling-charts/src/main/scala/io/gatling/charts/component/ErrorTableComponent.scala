/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.component

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.result.ErrorStats
import io.gatling.core.util.StringHelper

class ErrorTableComponent(errors: Seq[ErrorStats]) extends Component {

	def js = StringHelper.emptyFastring

	def html = if (errors.isEmpty)
		StringHelper.emptyFastring
	else
		fast"""<div class="statistics extensible-geant collapsed">
    <div class="title">
        <div class="title_collapsed" style="cursor: auto;">ERRORS</div>
    </div>
    <table id="container_errors" class="statistics-in extensible-geant">
        <thead>
            <tr>
                <th class="header"><span>Error</span></th>
                <th class="header"><span>Count</span></th>
                <th class="header"><span>Percentage</span></th>
            </tr>
        </thead>
		<tbody>
		    ${errors.map { error => fast"""<tr><td class="total">${error.message}</td><td class="value total">${error.count}</td><td class="value total">${error.percentage} %</td></tr>""" }.mkFastring}
		</tbody>
    </table>
</div>
"""

	def jsFiles: Seq[String] = Seq.empty
}
