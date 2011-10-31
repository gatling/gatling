package com.excilys.ebi.gatling.statistics.series

class PieSeries(name: String, data: List[(String, Int, String)]) extends Series(name) {

	override def toString =
		"""type: 'pie',
         name: '""" + name + """',
         data: [""" + data.map { e => "name: '" + e._1 + "', y: " + e._2 + ", color: '" + e._3 + "'" }.mkString("{ ", "},\n{", " }") + """],
         center: [100, 80],
         size: 100,
         showInLegend: true,
         dataLabels: {
            enabled: false
         }"""
}