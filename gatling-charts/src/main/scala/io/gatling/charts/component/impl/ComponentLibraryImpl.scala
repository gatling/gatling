/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.charts.component.impl

import io.gatling.charts.component.{ Component, ComponentLibrary }
import io.gatling.charts.stats._

/**
 * Mock implementation that is removed from the binary.
 * A unique implementation is expected to be present in the classpath.
 */
private[component] class ComponentLibraryImpl extends ComponentLibrary {

  def getAllUsersJs(runStart: Long, series: Series[IntVsTimePlot]): String = throw new UnsupportedOperationException
  def getActiveSessionsChartComponent(runStart: Long, series: Seq[Series[IntVsTimePlot]]): Component = throw new UnsupportedOperationException
  def getRequestsChartComponent(runStart: Long, counts: Series[CountsVsTimePlot], pieSeries: Series[PieSlice]): Component =
    throw new UnsupportedOperationException
  def getResponsesChartComponent(runStart: Long, counts: Series[CountsVsTimePlot], pieSeries: Series[PieSlice]): Component =
    throw new UnsupportedOperationException
  def getRequestDetailsResponseTimeChartComponent(runStart: Long, responseTimesSuccess: Series[PercentilesVsTimePlot]): Component =
    throw new UnsupportedOperationException
  def getRequestDetailsResponseTimeDistributionChartComponent(
      responseTimesSuccess: Series[PercentVsTimePlot],
      responseTimesFailures: Series[PercentVsTimePlot]
  ): Component = throw new UnsupportedOperationException
  def getRequestDetailsResponseTimeScatterChartComponent(successData: Series[IntVsTimePlot], failuresData: Series[IntVsTimePlot]): Component =
    throw new UnsupportedOperationException
  def getRequestDetailsIndicatorChartComponent: Component = throw new UnsupportedOperationException
  def getNumberOfRequestsChartComponent(numberOfRequestNames: Int): Component = throw new UnsupportedOperationException
  def getGroupDetailsDurationChartComponent(
      containerId: String,
      yAxisName: String,
      runStart: Long,
      durationsSuccess: Series[PercentilesVsTimePlot]
  ): Component = throw new UnsupportedOperationException
  def getGroupDetailsDurationDistributionChartComponent(
      title: String,
      containerId: String,
      durationsSuccess: Series[PercentVsTimePlot],
      durationsFailure: Series[PercentVsTimePlot]
  ): Component = throw new UnsupportedOperationException
}
