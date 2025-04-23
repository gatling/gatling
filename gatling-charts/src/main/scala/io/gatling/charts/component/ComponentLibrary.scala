/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import java.util.ServiceLoader

import scala.jdk.CollectionConverters._

import io.gatling.charts.report.GroupContainer
import io.gatling.charts.stats._

import com.typesafe.scalalogging.StrictLogging

private[charts] object ComponentLibrary extends StrictLogging {
  val Instance: ComponentLibrary =
    ServiceLoader.load(classOf[ComponentLibrary], getClass.getClassLoader).iterator().asScala.toList match {
      case Nil         => throw new IllegalStateException("Couldn't find a ComponentLibrary implementation")
      case single :: _ => single
      case multiple    => throw new IllegalStateException(s"Found multiple ComponentLibrary implementations: $multiple")
    }
}

private[gatling] trait ComponentLibrary {
  def getAllUsersJs(runStart: Long, series: Series[IntVsTimePlot]): String
  def getActiveSessionsComponent(runStart: Long, series: Seq[Series[IntVsTimePlot]]): Component
  def getRangesComponent(chartTitle: String, eventName: String, ranges: Ranges, large: Boolean): Component
  def getRequestCountPolarComponent(rootContainer: GroupContainer): Component
  def getDistributionComponent(
      title: String,
      yAxisName: String,
      durationsSuccess: Series[PercentVsTimePlot],
      durationsFailure: Series[PercentVsTimePlot]
  ): Component
  def getPercentilesOverTimeComponent(yAxisName: String, runStart: Long, successSeries: Series[PercentilesVsTimePlot]): Component
  def getRequestsComponent(runStart: Long, counts: Series[CountsVsTimePlot], pieSeries: Series[PieSlice]): Component
  def getResponsesComponent(runStart: Long, counts: Series[CountsVsTimePlot], pieSeries: Series[PieSlice]): Component
  def getResponseTimeScatterComponent(successData: Series[IntVsTimePlot], failuresData: Series[IntVsTimePlot]): Component
}
