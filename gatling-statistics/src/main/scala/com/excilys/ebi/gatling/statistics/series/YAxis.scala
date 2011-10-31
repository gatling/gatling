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