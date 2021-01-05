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

package io.gatling.charts.component

import scala.jdk.CollectionConverters._

import io.gatling.charts.component.impl.ComponentLibraryImpl
import io.gatling.charts.stats._

import com.typesafe.scalalogging.StrictLogging

private[charts] object ComponentLibrary extends StrictLogging {

  val Instance: ComponentLibrary = {

    val StaticLibraryBinderPath = "io/gatling/charts/component/impl/ComponentLibraryImpl.class"

    val paths = Option(getClass.getClassLoader)
      .map(_.getResources(StaticLibraryBinderPath))
      .getOrElse(ClassLoader.getSystemResources(StaticLibraryBinderPath))
      .asScala
      .toList

    if (paths.size > 1) {
      logger.warn("Class path contains multiple ComponentLibrary bindings")
      paths.foreach(url => logger.warn(s"Found ComponentLibrary binding in $url"))
    }

    new ComponentLibraryImpl
  }
}

private[gatling] trait ComponentLibrary {
  def getAllUsersJs(runStart: Long, series: Series[IntVsTimePlot]): String
  def getActiveSessionsChartComponent(runStart: Long, series: Seq[Series[IntVsTimePlot]]): Component
  def getRequestsChartComponent(runStart: Long, counts: Series[CountsVsTimePlot], pieSeries: Series[PieSlice]): Component
  def getResponsesChartComponent(runStart: Long, counts: Series[CountsVsTimePlot], pieSeries: Series[PieSlice]): Component
  def getRequestDetailsResponseTimeChartComponent(runStart: Long, responseTimesSuccess: Series[PercentilesVsTimePlot]): Component
  def getRequestDetailsResponseTimeDistributionChartComponent(
      responseTimesSuccess: Series[PercentVsTimePlot],
      responseTimesFailures: Series[PercentVsTimePlot]
  ): Component
  def getRequestDetailsResponseTimeScatterChartComponent(successData: Series[IntVsTimePlot], failuresData: Series[IntVsTimePlot]): Component
  def getRequestDetailsIndicatorChartComponent: Component
  def getNumberOfRequestsChartComponent(numberOfRequestNames: Int): Component
  def getGroupDetailsDurationChartComponent(containerId: String, yAxisName: String, runStart: Long, durationsSuccess: Series[PercentilesVsTimePlot]): Component
  def getGroupDetailsDurationDistributionChartComponent(
      title: String,
      containerId: String,
      durationsSuccess: Series[PercentVsTimePlot],
      durationsFailure: Series[PercentVsTimePlot]
  ): Component
}
