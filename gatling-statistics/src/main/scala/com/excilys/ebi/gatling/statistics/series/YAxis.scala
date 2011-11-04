/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.statistics.series

class YAxis(title: String, unit: String, opposite: Boolean, color: String = "#808080", plotBands: List[PlotBand] = Nil) {

	override def toString =
		"""title: { text: '""" + title + """', style: { color: '""" + color + """' } },
		plotLines: [{ value: 0, width: 1, color:'""" + color + """' }],
		labels: { formatter: function() { return this.value +' """ + unit + """'; }, style: { color: '""" + color + """' } },
		min: 0
		""" + plotBandsAsString + oppositeAsString

	def plotBandsAsString = if (!plotBands.isEmpty)
		", plotBands: [" + plotBands.mkString("{", "}, {", "}") + "]"
	else
		""

	def oppositeAsString = if (opposite)
		", opposite: true"
	else
		", opposite: false"
}