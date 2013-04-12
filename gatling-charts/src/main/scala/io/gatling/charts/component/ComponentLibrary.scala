/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.collection.JavaConversions.enumerationAsScalaIterator

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.charts.component.impl.ComponentLibraryImpl
import io.gatling.core.result.{ IntRangeVsTimePlot, IntVsTimePlot, PieSlice, Series }

object ComponentLibrary extends Logging {

	val instance: ComponentLibrary = {

		val STATIC_LIBRARY_BINDER_PATH = "com/excilys/ebi/gatling/charts/component/impl/ComponentLibraryImpl.class"

		val paths = Option(getClass.getClassLoader)
			.map(_.getResources(STATIC_LIBRARY_BINDER_PATH))
			.getOrElse(ClassLoader.getSystemResources(STATIC_LIBRARY_BINDER_PATH))
			.toList

		if (paths.size > 1) {
			logger.warn("Class path contains multiple ComponentLibrary bindings")
			paths.foreach(logger.warn(s"Found ComponentLibrary binding in $_"))
		}

		new ComponentLibraryImpl
	}
}

trait ComponentLibrary {
	def getAllSessionsJs(runStart: Long, series: Series[IntVsTimePlot]): String
	def getActiveSessionsChartComponent(runStart: Long, series: Seq[Series[IntVsTimePlot]]): Component
	def getRequestsChartComponent(runStart: Long, allRequests: Series[IntVsTimePlot], failedRequests: Series[IntVsTimePlot], succeededRequests: Series[IntVsTimePlot], pieSeries: Series[PieSlice]): Component
	def getTransactionsChartComponent(runStart: Long, allTransactions: Series[IntVsTimePlot], failedTransactions: Series[IntVsTimePlot], succeededTransactions: Series[IntVsTimePlot], pieSeries: Series[PieSlice]): Component
	def getRequestDetailsResponseTimeChartComponent(runStart: Long, responseTimesSuccess: Series[IntRangeVsTimePlot], responseTimesFailures: Series[IntRangeVsTimePlot]): Component
	def getRequestDetailsResponseTimeDistributionChartComponent(responseTimesSuccess: Series[IntVsTimePlot], responseTimesFailures: Series[IntVsTimePlot]): Component
	def getRequestDetailsLatencyChartComponent(runStart: Long, latencySuccess: Series[IntRangeVsTimePlot], latencyFailures: Series[IntRangeVsTimePlot]): Component
	def getRequestDetailsScatterChartComponent(successData: Series[IntVsTimePlot], failuresData: Series[IntVsTimePlot]): Component
	def getRequestDetailsIndicatorChartComponent: Component
	def getNumberOfRequestsChartComponent: Component
	def getGroupDurationChartComponent(runStart: Long, durationsSuccess: Series[IntRangeVsTimePlot], durationsFailure: Series[IntRangeVsTimePlot]): Component
	def getGroupDetailsDurationDistributionChartComponent(durationsSuccess: Series[IntVsTimePlot], durationsFailure: Series[IntVsTimePlot]): Component
}