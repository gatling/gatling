/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.charts.component

import scala.collection.JavaConversions.enumerationAsScalaIterator

import com.excilys.ebi.gatling.charts.component.impl.ComponentLibraryImpl
import com.excilys.ebi.gatling.charts.series.Series

import grizzled.slf4j.Logging

object ComponentLibrary extends Logging {

	val instance: ComponentLibrary = {

		val STATIC_LIBRARY_BINDER_PATH = "com/excilys/ebi/gatling/charts/component/impl/ComponentLibraryImpl.class"

		val paths = Option(getClass.getClassLoader)
			.map(_.getResources(STATIC_LIBRARY_BINDER_PATH))
			.getOrElse(ClassLoader.getSystemResources(STATIC_LIBRARY_BINDER_PATH))
			.toList

		if (paths.size > 1) {
			warn("Class path contains multiple ComponentLibrary bindings")
			paths.foreach(url => warn("Found ComponentLibrary binding in " + url))
		}

		new ComponentLibraryImpl
	}
}

abstract class ComponentLibrary {
	def getAllSessionsJs(runStart: Long, series: Series[Int, Int]): String
	def getActiveSessionsChartComponent(runStart: Long, series: Seq[Series[Int, Int]]): Component
	def getRequestsChartComponent(runStart: Long, allRequests: Series[Int, Int], failedRequests: Series[Int, Int], succeededRequests: Series[Int, Int], pieSeries: Series[String, Int]): Component
	def getTransactionsChartComponent(runStart: Long, allTransactions: Series[Int, Int], failedTransactions: Series[Int, Int], succeededTransactions: Series[Int, Int], pieSeries: Series[String, Int]): Component
	def getRequestDetailsResponseTimeChartComponent(runStart: Long, responseTimesSuccess: Series[Int, (Int, Int)], responseTimesFailures: Series[Int, (Int, Int)]): Component
	def getRequestDetailsResponseTimeDistributionChartComponent(responseTimesSuccess: Series[Int, Int], responseTimesFailures: Series[Int, Int]): Component
	def getRequestDetailsLatencyChartComponent(runStart: Long, latencySuccess: Series[Int, (Int, Int)], latencyFailures: Series[Int, (Int, Int)]): Component
	def getRequestDetailsScatterChartComponent(successData: Series[Int, Int], failuresData: Series[Int, Int]): Component
	def getRequestDetailsIndicatorChartComponent: Component
	def getNumberOfRequestsChartComponent: Component
}