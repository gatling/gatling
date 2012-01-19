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

class ComponentLibraryImpl extends ComponentLibrary {

	def getActiveSessionsChartComponent(series: Series[Long, Int]*): Component = throw new UnsupportedOperationException
	def getRequestsChartComponent(allRequests: Series[Long, Int], failedRequests: Series[Long, Int], succeededRequests: Series[Long, Int], pieSeries: Series[String, Int], allActiveSessions: Series[Long, Int]): Component = throw new UnsupportedOperationException
	def getTransactionsChartComponent(allTransactions: Series[Long, Int], failedTransactions: Series[Long, Int], succeededTransactions: Series[Long, Int], pieSeries: Series[String, Int], allActiveSessions: Series[Long, Int]): Component = throw new UnsupportedOperationException
	def getRequestDetailsResponseTimeChartComponent(responseTimesSuccess: Series[Long, Int], responseTimesFailures: Series[Long, Int], allActiveSessions: Series[Long, Int]): Component = throw new UnsupportedOperationException
	def getRequestDetailsLatencyChartComponent(latencySuccess: Series[Long, Int], latencyFailures: Series[Long, Int], allActiveSessions: Series[Long, Int]): Component = throw new UnsupportedOperationException
	def getRequestDetailsScatterChartComponent(successData: Series[Int, Long], failuresData: Series[Int, Long]): Component = throw new UnsupportedOperationException
	def getRequestDetailsIndicatorChartComponent(columnSeries: Series[String, Int], pieSeries: Series[String, Int]): Component = throw new UnsupportedOperationException
}