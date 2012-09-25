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
package com.excilys.ebi.gatling.charts.component.impl

import com.excilys.ebi.gatling.charts.component.{ ComponentLibrary, Component }
import com.excilys.ebi.gatling.charts.series.Series

/**
 * Mock implementation that is removed from the binary.
 * A unique implementation is expected to be present in the classpath.
 *
 * @author stephanelandelle
 */
class ComponentLibraryImpl extends ComponentLibrary {

	def getAllSessionsJs(runStart: Long, series: Series[Int, Int]): String = throw new UnsupportedOperationException
	def getActiveSessionsChartComponent(runStart: Long, series: Seq[Series[Int, Int]]): Component = throw new UnsupportedOperationException
	def getRequestsChartComponent(runStart: Long, allRequests: Series[Int, Int], failedRequests: Series[Int, Int], succeededRequests: Series[Int, Int], pieSeries: Series[String, Int]): Component = throw new UnsupportedOperationException
	def getTransactionsChartComponent(runStart: Long, allTransactions: Series[Int, Int], failedTransactions: Series[Int, Int], succeededTransactions: Series[Int, Int], pieSeries: Series[String, Int]): Component = throw new UnsupportedOperationException
	def getRequestDetailsResponseTimeChartComponent(runStart: Long, responseTimesSuccess: Series[Int, (Int, Int)], responseTimesFailures: Series[Int, (Int, Int)]): Component = throw new UnsupportedOperationException
	def getRequestDetailsResponseTimeDistributionChartComponent(responseTimesSuccess: Series[Int, Int], responseTimesFailures: Series[Int, Int]): Component = throw new UnsupportedOperationException
	def getRequestDetailsLatencyChartComponent(runStart: Long, latencySuccess: Series[Int, (Int, Int)], latencyFailures: Series[Int, (Int, Int)]): Component = throw new UnsupportedOperationException
	def getRequestDetailsScatterChartComponent(successData: Series[Int, Int], failuresData: Series[Int, Int]): Component = throw new UnsupportedOperationException
	def getRequestDetailsIndicatorChartComponent: Component = throw new UnsupportedOperationException
	def getNumberOfRequestsChartComponent: Component = throw new UnsupportedOperationException
}