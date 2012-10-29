package com.excilys.ebi.gatling.charts.result

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration

package object reader {

	val accuracyAsDouble = configuration.charting.accuracy.toDouble

	def reduceAccuracy(time: Int): Int = math.round(time / accuracyAsDouble).toInt * configuration.charting.accuracy
}
