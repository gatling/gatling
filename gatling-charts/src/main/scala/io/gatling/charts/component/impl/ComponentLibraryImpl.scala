/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
private[component] final class ComponentLibraryImpl extends ComponentLibrary {
  override def getAllUsersJs(runStart: Long, series: Series[IntVsTimePlot]): String = throw new UnsupportedOperationException
  override def getActiveSessionsComponent(runStart: Long, series: Seq[Series[IntVsTimePlot]]): Component = throw new UnsupportedOperationException
  override def getRangesComponent(chartTitle: String, eventName: String): Component = throw new UnsupportedOperationException
  override def getRequestCountPolarComponent: Component = throw new UnsupportedOperationException
  override def getDistributionComponent(
      title: String,
      yAxisName: String,
      durationsSuccess: Series[PercentVsTimePlot],
      durationsFailure: Series[PercentVsTimePlot]
  ): Component = throw new UnsupportedOperationException
  override def getPercentilesOverTimeComponent(
      yAxisName: String,
      runStart: Long,
      durationsSuccess: Series[PercentilesVsTimePlot]
  ): Component = throw new UnsupportedOperationException
  override def getRequestsComponent(runStart: Long, counts: Series[CountsVsTimePlot], pieSeries: Series[PieSlice]): Component =
    throw new UnsupportedOperationException
  override def getResponsesComponent(runStart: Long, counts: Series[CountsVsTimePlot], pieSeries: Series[PieSlice]): Component =
    throw new UnsupportedOperationException
  override def getResponseTimeScatterComponent(successData: Series[IntVsTimePlot], failuresData: Series[IntVsTimePlot]): Component =
    throw new UnsupportedOperationException
}
