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

		val paths = (Option(this.getClass.getClassLoader) match {
			case Some(classloader) => classloader.getResources(STATIC_LIBRARY_BINDER_PATH)
			case None => ClassLoader.getSystemResources(STATIC_LIBRARY_BINDER_PATH)
		}).toList

		if (paths.size > 1) {
			warn("Class path contains multiple ComponentLibrary bindings")
			paths.foreach(url => warn("Found ComponentLibrary binding in " + url))
		}

		new ComponentLibraryImpl
	}
}

abstract class ComponentLibrary {

	def getAllSessionsJs(series: Series[Long, Long]): String

	def getActiveSessionsChartComponent(series: Seq[Series[Long, Long]]): Component

	def getRequestsChartComponent(allRequests: Series[Long, Long], failedRequests: Series[Long, Long], succeededRequests: Series[Long, Long], pieSeries: Series[String, Long]): Component

	def getTransactionsChartComponent(allTransactions: Series[Long, Long], failedTransactions: Series[Long, Long], succeededTransactions: Series[Long, Long], pieSeries: Series[String, Long]): Component

	def getRequestDetailsResponseTimeChartComponent(responseTimesSuccess: Series[Long, Long], responseTimesFailures: Series[Long, Long]): Component

	def getRequestDetailsResponseTimeDistributionChartComponent(responseTimesSuccess: Series[Long, Long], responseTimesFailures: Series[Long, Long]): Component

	def getRequestDetailsLatencyChartComponent(latencySuccess: Series[Long, Long], latencyFailures: Series[Long, Long]): Component

	def getRequestDetailsScatterChartComponent(successData: Series[Long, Long], failuresData: Series[Long, Long]): Component

	def getRequestDetailsIndicatorChartComponent: Component
}